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

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
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
}
