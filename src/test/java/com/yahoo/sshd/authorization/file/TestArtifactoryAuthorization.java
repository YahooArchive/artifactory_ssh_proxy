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
package com.yahoo.sshd.authorization.file;

import java.io.IOException;
import java.util.List;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;
import com.yahoo.sshd.authorization.ArtifactoryPermTargetType;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.utils.RunnableComponent;

@Test(groups = "unit")
public class TestArtifactoryAuthorization {

    private FileBasedArtifactoryAuthorizer artifactoryAuthorization;

    @BeforeClass
    public void init() {
        SshdSettingsInterface mockedSshdSettings = new SshdSettingsInterface() {
            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public String getHostKeyPath() {
                return null;
            }

            @Override
            public DelegatingCommandFactory getCommandFactory() {
                return null;
            }

            @Override
            public List<DelegatingCommandFactory> getCfInstances() {
                return null;
            }

            @Override
            public MultiUserPKAuthenticator getPublickeyAuthenticator() throws IOException, InterruptedException {
                return null;
            }

            @Override
            public Factory<Command> getShellFactory() {
                return null;
            }

            @Override
            public ArtifactoryInformation getArtifactoryInfo() {
                return null;
            }

            @Override
            public int getNioWorkers() {
                return 0;
            }

            @Override
            public List<NamedFactory<Cipher>> getCiphers() {
                return null;
            }

            @Override
            public RunnableComponent[] getExternalComponents() {
                return null;
            }

            @Override
            public String getArtifactoryAuthorizationFilePath() {
                return "src/test/resources/auth/auth.txt";
            }

            @Override
            public String getRequestLogPath() {
                return null;
            }

            @Override
            public int getHttpPort() {
                return 0;
            }

        };
        artifactoryAuthorization = new FileBasedArtifactoryAuthorizer(mockedSshdSettings);
    }

    @SuppressWarnings("boxing")
    @DataProvider(name = "authSample")
    public static Object[][] getAuthSample() {
        return new Object[][] {
                        // Success cases
                        // repo & user found. user has read perm
                        {"repoX", "a", ArtifactoryPermTargetType.READ, true},
                        // repo & user found. user has write perm
                        {"repoX", "a", ArtifactoryPermTargetType.WRITE, true},
                        // repo found with no user but all READ
                        {"repoX", "c", ArtifactoryPermTargetType.READ, true},

                        // failing cases
                        // repo not found case
                        {"repoA", "charlesk", ArtifactoryPermTargetType.WRITE, false},
                        // repo found but user not found case
                        {"repoX", "user2", ArtifactoryPermTargetType.WRITE, false},
                        // repo found but no wrte perm
                        {"repoX", "c", ArtifactoryPermTargetType.WRITE, false}};
    }

    @Test(dataProvider = "authSample")
    public void testAuthorization(String repositoryName, String userName, ArtifactoryPermTargetType permissionTarget,
                    boolean expected) {
        boolean actual = artifactoryAuthorization.authorized(repositoryName, userName, permissionTarget);
        Assert.assertEquals(expected, actual);
    }

}
