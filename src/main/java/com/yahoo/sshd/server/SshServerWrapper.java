package com.yahoo.sshd.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.forward.DefaultTcpipForwarderFactory;
import org.apache.sshd.common.util.CloseableUtils;
import org.apache.sshd.common.util.CloseableUtils.AbstractCloseable;
import org.apache.sshd.server.keyprovider.PEMHostKeyProviderFactory;
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

public class SshServerWrapper implements Runnable, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshServerWrapper.class);

    private final SshdSettingsInterface settings;
    private final SshServer sshd;
    private final Injector injector;

    private final InjectableArtifactoryFileSystemFactory afFsFactory;

    private final KeyPairProvider keyPairProvider;
    private final SshRequestLog requestLog;

    private final CountDownLatch countdownLatch = new CountDownLatch(1);

    public SshServerWrapper(String[] args) throws SshdConfigurationException {
        /*
         * Guice.createInjector() takes your Modules, and returns a new Injector instance. Most applications will call
         * this method exactly once, in their main() method.
         */
        SshdSettingsModule sshdSettingsModule = new SshdSettingsModule();
        this.injector = Guice.createInjector(sshdSettingsModule);

        SshdSettingsFactory settingsFactory = injector.getInstance(SshdSettingsFactory.class);
        this.settings = settingsFactory.createSshdSettings(args);

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

        try {
            requestLog.setup();
        } catch (Exception e) {
            LOGGER.error("failed to set up request log", e);
            throw new RuntimeException("start failed", e);
        }
    }

    @Override
    public void run() {
        for (RunnableComponent component : settings.getExternalComponents()) {
            try {
                ThreadUtils.externalsThreadPool().submit(component);
            } catch (Exception e) {
                LOGGER.error("start of " + component.getName() + " failed", e);
                throw new RuntimeException("start failed", e);
            }
        }

        LOGGER.info("Starting SSHD on port " + settings.getPort());
        try {
            sshd.start();
        } catch (IOException e) {
            LOGGER.error("start failed", e);
            throw new RuntimeException("start failed", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            LOGGER.info("Stopping external services");
            // stop jetty et. al.
            // reverse the order so we stop in the opposite order we started.
            List<RunnableComponent> list = Arrays.asList(settings.getExternalComponents());
            Collections.reverse(list);

            for (RunnableComponent rc : list) {
                try {
                    rc.close();
                } catch (IOException e) {
                    LOGGER.info("close  " + rc + " failed ", e);
                }
            }

            LOGGER.info("Stopping sshd");
            sshd.stop();

            try {
                // close request log file
                requestLog.close();
            } catch (Exception e) {
                LOGGER.warn("failed to clean up requestLog", e);
            }

            LOGGER.info("Exiting");
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            countdownLatch.countDown();
        }
    }

    public CountDownLatch getLatch() {
        return countdownLatch;
    }
}
