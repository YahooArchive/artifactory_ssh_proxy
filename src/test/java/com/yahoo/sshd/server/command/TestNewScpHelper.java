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

package com.yahoo.sshd.server.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.filesystem.ArtifactorySshFile;
import com.yahoo.sshd.server.filesystem.NameLengthTuple;
import com.yahoo.sshd.server.logging.LoggingHelper;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelper;

public class TestNewScpHelper {

    private InputStream in = Mockito.mock(InputStream.class);
    private final OutputStream out = Mockito.mock(OutputStream.class);
    private FileSystemView root = Mockito.mock(FileSystemView.class);
    private JFrogArtifactoryClientHelper jfach = Mockito.mock(JFrogArtifactoryClientHelper.class);
    private LoggingHelper loggingHelper = Mockito.mock(LoggingHelper.class);

    @Test(groups = "unit", description = "reset the file size when meta-data is null")
    public void testUploadSshFileSize() throws Exception {
        ArtifactorySshFile path = new ArtifactorySshFile(jfach, null, null, null, null);
        Assert.assertEquals(path.getSize(), 0);
        NewScpHelper scpHelper = new NewScpHelper(in, out, root, loggingHelper);
        scpHelper.resetArtifactorySshFileSize(path, 10002);
        Assert.assertEquals(path.getSize(), 10002);
    }

    @Test(groups = "unit", description = "we don't care if parent exists")
    public void testParentDoesNotExist() throws IOException {
        NewScpHelper scpHelper = new NewScpHelper(in, out, root, loggingHelper);
        SshFile mavenMetaParent = new SshFileMockBuilder().build();
        SshFile mavenMetaData = new SshFileMockBuilder().parentFile(mavenMetaParent).build();
        NameLengthTuple nameLength = Mockito.mock(NameLengthTuple.class);

        try {
            SshFile file = scpHelper.validateFile(mavenMetaData, nameLength);
            Assert.assertNotNull(file);
        } catch (IOException e) {
            Assert.fail("Should not have thrown IOException");
        }
    }

    @Test(groups = "unit", expectedExceptions = IOException.class, description = "Expects not writable")
    public void testFileExistsAndNotWritable() throws IOException {
        NewScpHelper scpHelper = new NewScpHelper(in, out, root, loggingHelper);
        SshFile sshFileParent = new SshFileMockBuilder().build();
        SshFile sshFile =
                        new SshFileMockBuilder().isWritable(false).doesExist(true).isFile(true)
                                        .parentFile(sshFileParent).build();
        NameLengthTuple nameLength = Mockito.mock(NameLengthTuple.class);
        scpHelper.validateFile(sshFile, nameLength);
    }

    @Test(groups = "unit", description = "Expects writable")
    public void testFileExistsAndWritable() throws IOException {
        NewScpHelper scpHelper = new NewScpHelper(in, out, root, loggingHelper);
        SshFile sshFileParent = new SshFileMockBuilder().build();
        SshFile sshFile =
                        new SshFileMockBuilder().isWritable(true).doesExist(true).isFile(true)
                                        .parentFile(sshFileParent).build();
        NameLengthTuple nameLength = Mockito.mock(NameLengthTuple.class);
        Assert.assertNotNull(scpHelper.validateFile(sshFile, nameLength));
    }

    @DataProvider
    public Object[][] data() {
        return new Object[][] { {"C0644 2327209773 cdxcore_10.10.14.Linux.tar.gz"},
                        {"C0644 2614716 ant-1.7.1-13_1.el6.x86_64.rpm"}};
    }

    @Test(dataProvider = "data")
    public void testValidatePerms(String header) throws IOException {
        NewScpHelper scpHelper = new NewScpHelper(in, out, root, loggingHelper);
        NameLengthTuple nameLength = scpHelper.validatePerms(header);
        Assert.assertNotNull(nameLength);
    }



    public static final class SshFileMockBuilder {
        boolean doesExist = false;
        boolean isDirectory = true;
        boolean isFile = false;
        boolean isWritable = true;
        SshFile parentFile = null;

        public SshFileMockBuilder doesExist(boolean doesExist) {
            this.doesExist = doesExist;
            return this;
        }

        public SshFileMockBuilder isDirectory(boolean isDirectory) {
            this.isDirectory = isDirectory;
            this.isFile = !this.isDirectory;
            return this;
        }

        public SshFileMockBuilder isFile(boolean isFile) {
            this.isFile = isFile;
            this.isDirectory = !this.isFile;
            return this;
        }

        public SshFileMockBuilder isWritable(boolean isWritable) {
            this.isWritable = isWritable;
            return this;
        }

        public SshFileMockBuilder parentFile(SshFile parentFile) {
            this.parentFile = parentFile;
            return this;
        }

        @SuppressWarnings("boxing")
        public SshFile build() {
            SshFile path = Mockito.mock(SshFile.class);
            Mockito.when(path.doesExist()).thenReturn(doesExist);
            Mockito.when(path.isFile()).thenReturn(isFile);
            Mockito.when(path.isDirectory()).thenReturn(isDirectory);
            Mockito.when(path.getParentFile()).thenReturn(parentFile);
            Mockito.when(path.isWritable()).thenReturn(isWritable);

            return path;
        }
    }

}
