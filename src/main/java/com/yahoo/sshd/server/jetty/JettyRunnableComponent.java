package com.yahoo.sshd.server.jetty;

import org.eclipse.jetty.server.Server;

import com.yahoo.sshd.utils.RunnableComponent;

public class JettyRunnableComponent implements RunnableComponent {

    private int jettyPort;
    private String jettyWebAppDir;
    private Server server;

    public JettyRunnableComponent(final int jettyPort, final String jettyWebAppDir) {
        this.jettyPort = jettyPort;
        this.jettyWebAppDir = jettyWebAppDir;
    }

    @Override
    public void run() {
        try {
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

}
