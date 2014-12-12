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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.sshd.server.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;
import com.yahoo.sshd.authentication.file.FileBasedPKAuthenticator;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.server.shell.MessageShellFactory;
import com.yahoo.sshd.server.shell.SshProxyMessage;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.utils.RunnableComponent;

public class SshdProxySettings implements SshdSettingsInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshdProxySettings.class);

    protected final int port;
    protected final int httpPort;
    protected final String hostKeyPath;
    protected final List<DelegatingCommandFactory> cfInstances;

    protected final ArtifactoryInformation artifactoryInfo;

    protected final RunnableComponent[] externalComponents;

    protected final String artifactoryAuthorizationFilePath;
    protected final String requestLogFilePath;

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
            throw new SshdConfigurationException(
                            "invalid artifactory configuration, url, user and password must be specified");
        }
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

    static final boolean isLinux() {
        return "Linux".equalsIgnoreCase(System.getProperty("os.name"));
    }

    @Override
    public MultiUserPKAuthenticator getPublickeyAuthenticator() throws IOException, InterruptedException {

        // Make sure we sleep until this is ready
        CountDownLatch countdownLatch = new CountDownLatch(1);

        String authorizedUsers = System.getProperty("sshd.authorized_users", "");

        String[] authorizedUsersArray = authorizedUsers.split(",");

        Set<String> authorizedUsersSet;
        if (null == authorizedUsersArray || authorizedUsersArray.length == 0) {
            authorizedUsersSet = new HashSet<>();
        } else {
            authorizedUsersSet = new HashSet<>();
            authorizedUsersSet.addAll(Arrays.asList(authorizedUsersArray));
        }

        final MultiUserPKAuthenticator publickeyAuthenticator = getFileBasedAuth(countdownLatch);
        publickeyAuthenticator.start();

        LOGGER.info("Waiting for public keys  to be loaded");
        countdownLatch.await();
        LOGGER.info("Done waiting for public keys  to be loaded");

        return publickeyAuthenticator;
    }

    @Override
    public Factory<Command> getShellFactory() {
        // TODO when separating out settings, we'll provide a different success
        // message, or a file path for it.
        return new MessageShellFactory(SshProxyMessage.MESSAGE_STRING);
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

    // Save this code for later use. this is for scanning for authorized keys.
    // this is the non-sshd code.
    private MultiUserPKAuthenticator getFileBasedAuth(final CountDownLatch countdownLatch) throws IOException {
        final File keyHome = new File(System.getProperty("home", "/home/"));

        return new FileBasedPKAuthenticator(countdownLatch, keyHome, Arrays.asList(new Path[] {new File(
                        "/usr/local/sshproxy/").toPath()}));
    }

    /**
     * create a list of factories from a list of cipher names
     */
    @SuppressWarnings("unchecked")
    List<NamedFactory<Cipher>> createCipherFactoryList(List<String> cipherNames) {

        final NamedFactory<Cipher>[] cipherArray = new NamedFactory[] {new AES128CTR.Factory(), //
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
                LOGGER.info("Failed to load cipher " + cipherName, e);
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
}
