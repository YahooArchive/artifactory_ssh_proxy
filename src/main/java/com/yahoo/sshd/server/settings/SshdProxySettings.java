/*
 * Copyright 2014 Yahoo! Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the License); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.yahoo.sshd.server.settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.AES128CBC;
import org.apache.sshd.common.cipher.AES128CTR;
import org.apache.sshd.common.cipher.AES192CBC;
import org.apache.sshd.common.cipher.AES256CBC;
import org.apache.sshd.common.cipher.AES256CTR;
import org.apache.sshd.common.cipher.ARCFOUR128;
import org.apache.sshd.common.cipher.ARCFOUR256;
import org.apache.sshd.common.cipher.BlowfishCBC;
import org.apache.sshd.common.cipher.TripleDESCBC;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;
import com.yahoo.sshd.authentication.file.HomeDirectoryScanningPKAuthenticator;
import com.yahoo.sshd.authentication.file.LocalUserPKAuthenticator;
import com.yahoo.sshd.server.Sshd;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.server.shell.ForwardingShellFactory;
import com.yahoo.sshd.server.shell.GroovyShellFactory;
import com.yahoo.sshd.server.shell.MessageShellFactory;
import com.yahoo.sshd.server.shell.SshProxyMessage;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.utils.RunnableComponent;

public class SshdProxySettings implements SshdSettingsInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshdProxySettings.class);

    /**
     * Type of shell that gets created when someone ssh's in.
     */
    public static enum ShellMode {
        /**
         * Use {@link MessageShellFactory} to create a shell that simply presents a message.
         */
        MESSAGE,

        /**
         * Use {@link ForwardingShellFactory} to create a shell which echo's input back, but also allows forwarding.
         */
        FORWARDING_ECHO_SHELL,

        /**
         * Use {@link GroovyShellFactory} to create a shell supports groovy
         */
        GROOVY_SHELL
    }

    /**
     * The port the ssh server listens on.
     */
    protected final int port;

    /**
     * The port the optional jetty service listens on.
     */
    protected final int httpPort;

    /**
     * The path to the host key the server uses
     */
    protected final String hostKeyPath;

    /**
     * An odd hackery to allow the CommandFactories to be changed while doing development TODO: remove/refactor out.
     */
    protected final List<DelegatingCommandFactory> cfInstances;

    /**
     * Information required to connect to artifactory
     */
    protected final ArtifactoryInformation artifactoryInfo;

    /**
     * An array of external services that are running. {@link Sshd#start()} will iterate over these and start them.
     * {@link Sshd#stop()} will iterate over these and stop them.
     */
    protected final RunnableComponent[] externalComponents;

    protected final String artifactoryAuthorizationFilePath;
    protected final String requestLogFilePath;

    /**
     * True if in development mode, this is used for running locally to skip extra setup of auth.
     */
    protected final boolean developmentMode;

    /**
     * Type of shell that gets created when someone ssh's in.
     */
    protected final ShellMode shellMode;

    public SshdProxySettings(SshdSettingsBuilder b) throws SshdConfigurationException {

        this.port = b.getSshdPort();
        this.httpPort = b.getHttpPort();
        this.hostKeyPath = b.getHostKeyPath();
        this.cfInstances = Collections.unmodifiableList(b.getCommandFactories());

        String artifactoryUrl = b.getArtifactoryUrl();
        String artifactoryUsername = b.getArtifactoryUsername();
        String artifactoryPassword = b.getArtifactoryPassword();

        this.artifactoryInfo =
                        createArtifactoryInformation(b.getArtifactoryUrl(), b.getArtifactoryUsername(),
                                        b.getArtifactoryPassword());

        RunnableComponent[] temp = b.getExternalComponents();
        this.externalComponents = Arrays.copyOf(temp, temp.length);

        this.artifactoryAuthorizationFilePath = b.getArtifactoryAuthorizationFilePath();
        this.requestLogFilePath = b.getRequestLogPath();

        if (port <= 0 || port >= 65536) {
            throw new SshdConfigurationException("SSHD Port " + port + " is invalid");
        }

        // -1 means it's disabled
        if (httpPort >= 65536) {
            throw new SshdConfigurationException("HTTP Port " + httpPort + " is invalid");
        }

        if (null == artifactoryUrl || artifactoryUrl.isEmpty() || null == artifactoryUsername
                        || null == artifactoryPassword) {
            throw new SshdConfigurationException("invalid artifactory configuration, url: " + artifactoryUrl
                            + ", user: " + artifactoryUsername + " and password: " + artifactoryPassword
                            + "  must be specified");
        }

        this.developmentMode = b.getDevelopmentMode();

        this.shellMode = b.getShellMode();
    }

    /**
     * Override this to control how you access artifactory. For example if your artifactory instance doesn't use http
     * basic auth, and instead uses SAML or another authorization mechanism, you'll want to override this function to
     * return a different instances of {@link ArtifactoryInformation}
     * 
     * @param artifactoryUrl
     * @param artifactoryUsername
     * @param artifactoryPassword
     * @return
     */
    protected ArtifactoryInformation createArtifactoryInformation(final String artifactoryUrl,
                    final String artifactoryUsername, final String artifactoryPassword) {
        return new ArtifactoryInformation(artifactoryUrl, artifactoryUsername, artifactoryPassword);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.yahoo.sshd.server.SshdSettingsInterface#getPort()
     */
    @Override
    public int getPort() {
        return port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.yahoo.sshd.server.SshdSettingsInterface#getHostKeyPath()
     */
    @Override
    public String getHostKeyPath() {
        return hostKeyPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.yahoo.sshd.server.SshdSettingsInterface#getCommandFactory()
     */
    @Override
    public DelegatingCommandFactory getCommandFactory() {
        Stack<DelegatingCommandFactory> stack = new Stack<>();
        List<DelegatingCommandFactory> instances = getCfInstances();
        for (int i = instances.size() - 1; i >= 0; i--) {
            stack.push(instances.get(i));
        }

        DelegatingCommandFactory start = stack.pop();
        DelegatingCommandFactory current = start;

        while (!stack.isEmpty()) {
            current = (DelegatingCommandFactory) current.setDelegate(stack.pop());
        }

        return start;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.yahoo.sshd.server.SshdSettingsInterface#getCfInstances()
     */
    @Override
    public List<DelegatingCommandFactory> getCfInstances() {
        return cfInstances;
    }

    @Override
    public MultiUserPKAuthenticator getPublickeyAuthenticator() throws IOException, InterruptedException {
        /**
         * countdown is called on this latch when {@link MultiUserPKAuthenticator#start} is done.
         */
        CountDownLatch wakeupLatch = new CountDownLatch(1);

        final MultiUserPKAuthenticator publickeyAuthenticator = getFileBasedAuth(wakeupLatch);
        publickeyAuthenticator.start();

        LOGGER.info("Waiting for public keys  to be loaded");
        wakeupLatch.await();

        int loadedKeys = publickeyAuthenticator.getNumberOfKeysLoads();
        LOGGER.info("Loaded {} public keys", Integer.valueOf(loadedKeys));

        if (loadedKeys < 1) {
            throw new IOException("Didn't load any public keys, auth will not work, exiting");
        }

        return publickeyAuthenticator;
    }

    @Override
    public Factory<Command> getShellFactory() {
        EnumSet<TtyOptions> ttyOptions;

        if (OsUtils.isUNIX()) {
            /**
             * org.apache.sshd.server.shell.ProcessShellFactory does this: ttyOptions = EnumSet.of(TtyOptions.ONlCr);
             * 
             * However, it doesn't seem to work for me. So in our copy of
             * org.apache.sshd.server.shell.TtyFilterOutputStream.TtyFilterOutputStream(EnumSet<TtyOptions>,
             * OutputStream, TtyFilterInputStream), we have a special hack that if TtyOptions.INlCr and TtyOptions.ICrNl
             * are both set, send cr nl instead. no idea if the windows even works.
             */
            // ttyOptions = EnumSet.of(TtyOptions.ONlCr);
            ttyOptions = EnumSet.of(TtyOptions.OCrNl, TtyOptions.INlCr, TtyOptions.ICrNl);
        } else {
            ttyOptions = EnumSet.of(TtyOptions.Echo, TtyOptions.OCrNl, TtyOptions.INlCr, TtyOptions.ICrNl);
        }

        switch (shellMode) {
            case FORWARDING_ECHO_SHELL:
                return new ForwardingShellFactory(ttyOptions);

            case GROOVY_SHELL:
                return new GroovyShellFactory(ttyOptions);

            case MESSAGE:
            default:
                // TODO when separating out settings, we'll provide a different success
                // message, or a file path for it.
                return new MessageShellFactory(SshProxyMessage.MESSAGE_STRING);
        }
    }

    @Override
    public ArtifactoryInformation getArtifactoryInfo() {
        return artifactoryInfo;
    }

    @Override
    public int getNioWorkers() {
        // set number of NIO Workers.
        // we'll set this a little higher than the default of # of cpus.
        // FIXME: make this configuration
        // default was copied.`
        return Runtime.getRuntime().availableProcessors() + 1;
    }

    /**
     * Return the authenticator that should be used to get the keys
     * 
     * @param wakeupLatch Latch that countdown is called on when start is done registering everything.
     * @return an instance of {@link MultiUserPKAuthenticator} that can be used for authenticating users based on the
     *         public keys that have been loaded.
     * @throws IOException
     */
    protected MultiUserPKAuthenticator getFileBasedAuth(final CountDownLatch wakeupLatch) throws IOException {

        if (!isDevelopementMode()) {
            // Allow people to override the location of "home"
            final File keyHome = new File(System.getProperty("sshd_proxy.home", "/home/"));

            // by default we don't have anything to exclude from /home
            return new HomeDirectoryScanningPKAuthenticator(wakeupLatch, keyHome, Arrays.asList(new Path[] {}));
        } else {
            // in development mode, look up the home directory of the current user and use that authorized_keys.
            // the theory is, if you gave it -x, you are probably trying to play with it, and a single ssh user
            // and no repo whitelisting are very useful.
            return new LocalUserPKAuthenticator(wakeupLatch);
        }
    }

    /**
     * create a list of factories from a list of cipher names
     */
    @SuppressWarnings("unchecked")
    public static List<NamedFactory<Cipher>> createCipherFactoryList(List<String> cipherNames) {

        final NamedFactory<Cipher>[] cipherArray = new NamedFactory[] { //
                        //
                                        new AES128CTR.Factory(), //
                                        new AES256CTR.Factory(), //
                                        new ARCFOUR128.Factory(), //
                                        new ARCFOUR256.Factory(), //
                                        new AES128CBC.Factory(), //
                                        new TripleDESCBC.Factory(), //
                                        new BlowfishCBC.Factory(), //
                                        new AES192CBC.Factory(), //
                                        new AES256CBC.Factory(), //
                        };

        // first get all of the ciphers we know about in a set
        final Map<String, NamedFactory<Cipher>> nameMap = new HashMap<>();

        final boolean useDefaults;
        if (cipherNames.size() <= 0) {
            useDefaults = true;
            cipherNames = new ArrayList<>(cipherArray.length);
        } else {
            useDefaults = false;

        }

        for (NamedFactory<Cipher> cipherFactory : cipherArray) {
            nameMap.put(cipherFactory.getName(), cipherFactory);
            if (useDefaults) {
                cipherNames.add(cipherFactory.getName());
            }
        }

        final List<NamedFactory<Cipher>> available = new ArrayList<>(cipherArray.length);

        for (String cipherName : cipherNames) {
            final NamedFactory<Cipher> factory = nameMap.get(cipherName);
            if (null == factory) {
                continue;
            }


            try {
                final Cipher c = factory.create();
                final byte[] key = new byte[c.getBlockSize()];
                final byte[] iv = new byte[c.getIVSize()];
                c.init(Cipher.Mode.Encrypt, key, iv);
                available.add(factory);
            } catch (Exception e) {
                LOGGER.info("Failed to load cipher " + cipherName
                                + " ensure you have the unlimited strength JCE installed");
            }
        }

        return available;
    }

    @Override
    public List<NamedFactory<Cipher>> getCiphers() {
        // FIXME: get list of approved ciphers
        // FIXME: load cipher list from config
        // FIXME: only allow approved ciphers
        // see org.apache.sshd.SshServer.setUpDefaultCiphers(SshServer)

        return createCipherFactoryList(Collections.<String>emptyList());
    }

    @Override
    public RunnableComponent[] getExternalComponents() {
        return externalComponents;
    }

    @Override
    public String getArtifactoryAuthorizationFilePath() {
        return this.artifactoryAuthorizationFilePath;
    }

    @Override
    public String getRequestLogPath() {
        return requestLogFilePath;
    }

    @Override
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Added this to make doing dev work a lot less painful. I don't want to setup an auth.txt for doing local
     * development, so add a flag. The problem is, we probably should flag this separately, because later we might
     * create a developer version that hosts more bits. When that is done, we'll have to rethink this.
     */
    @Override
    public boolean isDevelopementMode() {
        return developmentMode;
    }
}
