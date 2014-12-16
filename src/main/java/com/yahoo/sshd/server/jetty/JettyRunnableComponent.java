package com.yahoo.sshd.server.jetty;

import java.io.IOException;

import org.eclipse.jetty.server.Server;

import com.yahoo.sshd.utils.RunnableComponent;

/**
 * A wrapper to allow jetty to be started and stopped.
 * 
 * @author areese
 * 
 */
public class JettyRunnableComponent implements RunnableComponent {

    private int jettyPort;
    private String jettyWebAppDir;
    private Server server;

    public JettyRunnableComponent(final int jettyPort, final String jettyWebAppDir) {
        this.jettyPort = jettyPort;
        this.jettyWebAppDir = (null == jettyWebAppDir) ? null : jettyWebAppDir.trim();
    }

    @Override
    public void run() {
        try {
            if (-1 == jettyPort || null == jettyWebAppDir || jettyWebAppDir.isEmpty()) {
                return;
            }

            server = JettyServer.newServer(jettyPort, jettyWebAppDir);
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "Jetty Server";
    }

    @Override
    public void close() throws IOException {
        if (null == server) {
            return;
        }

        try {
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            server = null;
        }
    }

}
