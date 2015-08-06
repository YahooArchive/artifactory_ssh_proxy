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

import java.io.File;
import java.lang.management.ManagementFactory;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServer {
    final static Logger LOG = LoggerFactory.getLogger(JettyServer.class);


    @SuppressWarnings({"resource", "boxing"})
    public static Server newServer(int jettyPort, String jettyWebAppDir, JettyServiceSetting jettyServiceSetting) throws Exception {

        if (jettyPort == 0 || jettyWebAppDir == null) {
            throw new IllegalArgumentException("Jetty port and resource dir may not be empty");
        }

        // server setup
        Server server = new Server();
        server.addBean(new ScheduledExecutorScheduler());
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);
        server.setStopAtShutdown(true);


        // http://www.eclipse.org/jetty/documentation/current/embedding-jetty.html#d0e19050
        // Setup JMX
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);


        //setup handlers according to the jetty settings
        HandlerCollection handlerCollection = new HandlerCollection();

        if (jettyServiceSetting == JettyServiceSetting.ARTIFACTORY || jettyServiceSetting == JettyServiceSetting.BOTH) {

            // The WebAppContext is the entity that controls the environment in
            // which a web application lives and breathes. In this example the
            // context path is being set to "/" so it is suitable for serving root
            // context requests and then we see it setting the location of the war.
            // A whole host of other configurations are available, ranging from
            // configuring to support annotation scanning in the webapp (through
            // PlusConfiguration) to choosing where the webapp will unpack itself.
            WebAppContext webapp = new WebAppContext();
            File warFile = new File(jettyWebAppDir + File.separator + "artifactory.war");
            webapp.setContextPath("/artifactory");
            webapp.setWar(warFile.getAbsolutePath());

            // A WebAppContext is a ContextHandler as well so it needs to be set to
            // the server so it is aware of where to send the appropriate requests.
            handlerCollection.addHandler(webapp);

        }

        if (jettyServiceSetting == JettyServiceSetting.VIP || jettyServiceSetting == JettyServiceSetting.BOTH) {

            // Serve resource files which reside in the jettyWebAppDir
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(false);
            resourceHandler.setResourceBase(jettyWebAppDir);

            handlerCollection.addHandler(resourceHandler);
        }

        server.setHandler(handlerCollection);


        // http configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSendServerVersion(true);

        // HTTP connector
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(jettyPort);
        server.addConnector(http);

        // start server
        server.start();

        LOG.info("Started jetty server on port: {}, resource dir: {} ", jettyPort, jettyWebAppDir);
        return server;
    }
}
