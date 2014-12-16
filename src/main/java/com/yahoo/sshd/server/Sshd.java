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
package com.yahoo.sshd.server;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.forward.DefaultTcpipForwarderFactory;
import org.apache.sshd.common.util.CloseableUtils;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.keyprovider.PEMHostKeyProviderFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yahoo.sshd.authorization.ArtifactoryAuthorizerProviderFactory;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.server.configuration.DenyingForwardingFilter;
import com.yahoo.sshd.server.filesystem.InjectableArtifactoryFileSystemFactory;
import com.yahoo.sshd.server.logging.RequestLogFactory;
import com.yahoo.sshd.server.logging.SshRequestLog;
import com.yahoo.sshd.server.settings.SshdConfigurationException;
import com.yahoo.sshd.server.settings.SshdSettingsFactory;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;
import com.yahoo.sshd.server.settings.SshdSettingsModule;
import com.yahoo.sshd.utils.RunnableComponent;
import com.yahoo.sshd.utils.ThreadUtils;

public class Sshd implements Daemon, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sshd.class);

    /*
     * ================================= Main class implementation =================================
     */

    /**
     * used when we are launched from jsvc
     */
    private String[] args;

    // TODO not sure we need this.
    private SshdSettingsFactory settingsFactory;

    private SshdSettingsInterface settings;
    private SshServer sshd;
    private Injector injector;

    private InjectableArtifactoryFileSystemFactory afFsFactory;

    private KeyPairProvider keyPairProvider;
    private SshRequestLog requestLog;

    /**
     * constructor used by jsvc
     */
    public Sshd() {

    }

    /**
     * constructor used by main.
     * 
     * @param settingsFactory
     * @param args
     * @throws SshdConfigurationException
     */
    public Sshd(final String[] args) throws SshdConfigurationException {
        this.args = args;
        setup();
    }

    /**
     * We have to set things up in setup, because if we are called by jsvc, it creates us then call start, which is
     * where we call setup.
     * 
     * @throws SshdConfigurationException
     */
    protected void setup() throws SshdConfigurationException {
        /*
         * Guice.createInjector() takes your Modules, and returns a new Injector instance. Most applications will call
         * this method exactly once, in their main() method.
         */
        SshdSettingsModule sshdSettingsModule = new SshdSettingsModule();
        this.injector = Guice.createInjector(sshdSettingsModule);
        this.settingsFactory = injector.getInstance(SshdSettingsFactory.class);
        this.settings = this.settingsFactory.createSshdSettings(this.args);
        this.afFsFactory = injector.getInstance(InjectableArtifactoryFileSystemFactory.class);
        this.afFsFactory.setAfInfo(this.settings.getArtifactoryInfo());
        this.afFsFactory.setArtifactoryAuthorizer(injector.getInstance(ArtifactoryAuthorizerProviderFactory.class)
                        .artifactoryAuthorizerProvider(this.settings));
        this.keyPairProvider =
                        injector.getInstance(PEMHostKeyProviderFactory.class).createPEMHostKeyProvider(
                                        this.settings.getHostKeyPath());
        this.requestLog =
                        injector.getInstance(RequestLogFactory.class).createRequestLog(
                                        this.settings.getRequestLogPath());

        // Setup the request log for each command factory.
        for (DelegatingCommandFactory cf : this.settings.getCfInstances()) {
            cf.setRequestLog(this.requestLog);
        }

        LOGGER.debug("Got FS Factory: " + this.afFsFactory.getClass().getCanonicalName());
    }

    public static void main(String[] args) throws Exception {
        Sshd sshd = new Sshd(args);
        sshd.run();
    }

    @Override
    public void run() {

        // load bouncycastle first, and fail fast.
        Security.addProvider(new BouncyCastleProvider());
        if (!SecurityUtils.isBouncyCastleRegistered()) {
            throw new RuntimeException(
                            "BouncyCastle must be registered: http://www.bouncycastle.org/wiki/display/JA1/Provider+Installation");
        }

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(settings.getPort());

        sshd.setKeyPairProvider(this.keyPairProvider);

        // setup the executor to have more than 1 thread.
        sshd.setScheduledExecutorService(ThreadUtils.scheduledExecutorServer(), true);

        // setup the shell to only provide a message and nothing else.
        sshd.setShellFactory(settings.getShellFactory());

        // setup the filesystem view to make it look at artifactory instead of
        // the fs.
        sshd.setFileSystemFactory(afFsFactory);

        // this lets us do weirdness, which is to configure where we look for
        // commands
        sshd.setCommandFactory(settings.getCommandFactory());

        // DENY all ssh forwarding.
        sshd.setTcpipForwardingFilter(new DenyingForwardingFilter());

        sshd.setTcpipForwarderFactory(new DefaultTcpipForwarderFactory());

        // explicitly disable all auth except pkauth see
        // org.apache.sshd.SshServer.checkConfig()
        sshd.setPasswordAuthenticator(null);
        sshd.setGSSAuthenticator(null);

        // set number of NIO Workers.
        sshd.setNioWorkers(settings.getNioWorkers());

        // explicitly disable agentFactory
        sshd.setAgentFactory(null);

        // only allow specific ciphers if configured, otherwise defaults will be
        // used.
        sshd.setCipherFactories(settings.getCiphers());

        try {
            sshd.setPublickeyAuthenticator(settings.getPublickeyAuthenticator());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("PublicKey Auth loading failed", e);
            throw new RuntimeException("PublicKey Auth loading failed", e);
        }

        for (RunnableComponent component : settings.getExternalComponents()) {
            try {
                ThreadUtils.externalsThreadPool().submit(component);
            } catch (Exception e) {
                LOGGER.error("start of " + component.getName() + " failed", e);
                throw new RuntimeException("start failed", e);
            }
        }

        try {
            requestLog.setup();
        } catch (Exception e) {
            LOGGER.error("failed to set up request log", e);
            throw new RuntimeException("start failed", e);
        }

        LOGGER.info("Starting SSHD on port " + settings.getPort());
        try {
            sshd.start();
        } catch (IOException e) {
            LOGGER.error("start failed", e);
            throw new RuntimeException("start failed", e);
        }
    }

    /**
     * Initialize this <code>Daemon</code> instance.
     * <p>
     * This method gets called once the JVM process is created and the <code>Daemon</code> instance is created thru its
     * empty public constructor.
     * </p>
     * <p>
     * Under certain operating systems (typically Unix based operating systems) and if the native invocation framework
     * is configured to do so, this method might be called with <i>super-user</i> privileges.
     * </p>
     * <p>
     * For example, it might be wise to create <code>ServerSocket</code> instances within the scope of this method, and
     * perform all operations requiring <i>super-user</i> privileges in the underlying operating system.
     * </p>
     * <p>
     * Apart from set up and allocation of native resources, this method must not start the actual operation of the
     * <code>Daemon</code> (such as starting threads calling the <code>ServerSocket.accept()</code> method) as this
     * would impose some serious security hazards. The start of operation must be performed in the <code>start()</code>
     * method.
     * </p>
     * 
     * @param context A <code>DaemonContext</code> object used to communicate with the container.
     * @exception DaemonInitException An exception that prevented initialization where you want to display a nice
     *            message to the user, rather than a stack trace.
     * @exception Exception Any exception preventing a successful initialization.
     */
    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        // save our arguments
        this.args = arg0.getArguments();
    }

    /**
     * Start the operation of this <code>Daemon</code> instance. This method is to be invoked by the environment after
     * the init() method has been successfully invoked and possibly the security level of the JVM has been dropped.
     * Implementors of this method are free to start any number of threads, but need to return control after having done
     * that to enable invocation of the stop()-method.
     */
    @Override
    public void start() throws Exception {
        // now that we have args, and are ready to start, setup the settings
        setup();
        new Thread(this).start();
    }

    /**
     * Stop the operation of this <code>Daemon</code> instance. Note that the proper place to free any allocated
     * resources such as sockets or file descriptors is in the destroy method, as the container may restart the Daemon
     * by calling start() after stop().
     * 
     * @throws InterruptedException
     */
    @Override
    public void stop() throws InterruptedException, IOException {
        LOGGER.info("Stopping external services");
        // stop jetty et. al.
        // reverse the order so we stop in the opposite order we started.
        List<RunnableComponent> list = Arrays.asList(settings.getExternalComponents());
        Collections.reverse(list);
        CloseableUtils.sequential(list.toArray(new RunnableComponent[] {})).close(false).await();

        LOGGER.info("Stopping sshd");
        sshd.stop();

        LOGGER.info("Exiting");
    }

    /**
     * Free any resources allocated by this daemon such as file descriptors or sockets. This method gets called by the
     * container after stop() has been called, before the JVM exits. The Daemon can not be restarted after this method
     * has been called without a new call to the init() method.
     */
    @Override
    public void destroy() {
        cleanup();
    }

    protected void cleanup() {
        try {
            // close request log file
            requestLog.cleanup();
        } catch (Exception e) {
            LOGGER.warn("failed to clean up requestLog", e);
        }
    }
}
