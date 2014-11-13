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
package com.yahoo.sshd.server.filesystem;

import java.io.IOException;
import java.text.ParseException;

import junit.framework.Assert;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaData;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaDataParseFailureException;
import com.yahoo.sshd.tools.artifactory.ArtifactNotFoundException;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelper;

@Test(groups = "unit")
public class TestArtifactorySshFile {
    private JFrogArtifactoryClientHelper mockJfach;
    private ArtifactoryAuthorizer mockArtifactoryAuthorizer;
    private ArtifactMetaData mockMetaData;

    @BeforeSuite
    public void setUp() {
        mockJfach = Mockito.mock(JFrogArtifactoryClientHelper.class);
        mockArtifactoryAuthorizer = Mockito.mock(ArtifactoryAuthorizer.class);
        mockMetaData = Mockito.mock(ArtifactMetaData.class);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testGetSize() throws ArtifactNotFoundException, IOException, ArtifactMetaDataParseFailureException,
                    ParseException {
        // artifact doesn't exist
        Mockito.when(mockJfach.getArtifact(Matchers.anyString())).thenReturn(null);
        ArtifactorySshFile sshFile =
                        new ArtifactorySshFile(mockJfach, "upload.txt", "sshd_user", "maven", mockArtifactoryAuthorizer);
        Assert.assertEquals(sshFile.getSize(), 0);

        // artifact exists with certain size
        Mockito.when(mockJfach.getArtifact(Matchers.anyString())).thenReturn(mockMetaData);
        Mockito.when(mockMetaData.getSize()).thenReturn(1024L);
        ArtifactorySshFile sshFile2 =
                        new ArtifactorySshFile(mockJfach, "upload.txt", "sshd_user", "maven", mockArtifactoryAuthorizer);
        Assert.assertEquals(sshFile2.getSize(), 1024L);
    }

    @Test
    public void testGetRepoName() throws ArtifactNotFoundException, IOException, ArtifactMetaDataParseFailureException,
                    ParseException {
        ArtifactorySshFile sshFile =
                        new ArtifactorySshFile(mockJfach, "upload.txt", "sshd_user", "maven", mockArtifactoryAuthorizer);
        Assert.assertEquals(sshFile.getRepoName(), "maven");
    }

}
