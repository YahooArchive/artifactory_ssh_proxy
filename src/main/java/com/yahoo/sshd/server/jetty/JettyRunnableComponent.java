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
    private JettyServiceSetting jettyServiceSetting;

    public JettyRunnableComponent(final int jettyPort, final String jettyWebAppDir,
                                  final JettyServiceSetting jettyServiceSetting) {
        this.jettyPort = jettyPort;
        this.jettyServiceSetting = jettyServiceSetting;
        this.jettyWebAppDir = (null == jettyWebAppDir) ? null : jettyWebAppDir.trim();
    }

    @Override
    public void run() {
        try {
            if (-1 == jettyPort || null == jettyWebAppDir || jettyWebAppDir.isEmpty()) {
                return;
            }

            server = JettyServer.newServer(jettyPort, jettyWebAppDir, jettyServiceSetting);
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
