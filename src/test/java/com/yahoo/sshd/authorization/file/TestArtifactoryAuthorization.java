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

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.authorization.ArtifactoryPermTargetType;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;

@Test(groups = "unit")
public class TestArtifactoryAuthorization {

    private FileBasedArtifactoryAuthorizer artifactoryAuthorization;

    @BeforeClass
    public void init() {
        SshdSettingsInterface mockedSshdSettings = Mockito.mock(SshdSettingsInterface.class);
        Mockito.when(mockedSshdSettings.getArtifactoryAuthorizationFilePath()).thenReturn(
                        "src/test/resources/auth/auth.txt");
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
