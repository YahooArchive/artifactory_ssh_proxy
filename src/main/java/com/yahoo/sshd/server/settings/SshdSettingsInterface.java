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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.TcpipForwarderFactory;
import org.apache.sshd.server.Command;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.utils.RunnableComponent;

public interface SshdSettingsInterface {

    /**
     * @return port sshd is listening on
     */
    int getPort();

    String getHostKeyPath();

    DelegatingCommandFactory getCommandFactory();

    List<DelegatingCommandFactory> getCfInstances();

    MultiUserPKAuthenticator getPublickeyAuthenticator() throws IOException, InterruptedException;

    /**
     * Default shell just returns a message.
     * 
     * @return an implementation of the Factory that implements a shell
     */
    Factory<Command> getShellFactory();

    ArtifactoryInformation getArtifactoryInfo();

    /**
     * Get number of NIO Workers. Defaults to # of cpus +1
     * 
     * @return number of NIO workers the sshd server should use.
     */
    int getNioWorkers();

    List<NamedFactory<Cipher>> getCiphers();

    RunnableComponent[] getExternalComponents();

    String getArtifactoryAuthorizationFilePath();

    String getRequestLogPath();

    /**
     * @return port http is listening on.
     */
    int getHttpPort();

    /**
     * Make a special flag for people running it on the desktop and debugging who really don't want to setup a lot of
     * extra things that are required for production.
     * 
     * @return true if we are in development mode.
     */
    boolean isDevelopementMode();

    /**
     * 
     * @return Factory to use for filtering.
     */
    TcpipForwarderFactory getForwardingFactory();

    /**
     * 
     * @return an implementation of {@link ForwardingFilter} that can be used to see if forwarding is allowed.
     */
    ForwardingFilter getForwardingFilter();

    /**
     * This is used to determine what implementation of {@link ForwardingFilter}
     * {@link SshdSettingsInterface#getForwardingFilter()} should return
     * 
     * @return
     */
    boolean isForwardingAllowed();

    /**
     * 
     * @return a map of user env strings to artifactory properties.
     */
    Map<String, String> getEnvToAfPropertyMapping();
}
