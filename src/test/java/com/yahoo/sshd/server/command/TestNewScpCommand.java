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
import org.apache.sshd.common.scp.ScpHelper;
import org.apache.sshd.server.ExitCallback;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.logging.LoggingHelper;
import com.yahoo.sshd.server.logging.SshRequestLog;

@Test(groups = "unit")
public class TestNewScpCommand {

    @SuppressWarnings({"boxing", "resource"})
    NewScpCommand getFailureScpCommand(int returnVal, ExitCallback callback) throws IOException {
        String filePath = "/mep-testing-gradle/foo/bar/maven-metadata.xml";
        FileSystemView viewMock = Mockito.mock(FileSystemView.class);

        Mockito.when(viewMock.getFile(Matchers.any(SshFile.class), Matchers.anyString())).thenReturn(null);

        OutputStream osMock = Mockito.mock(OutputStream.class);
        InputStream isMock = Mockito.mock(InputStream.class);
        LoggingHelper loggingHelper = Mockito.mock(LoggingHelper.class);

        Mockito.when(isMock.read()).thenReturn(returnVal);

        final ScpHelper helperMocked = new NewScpHelper(isMock, osMock, viewMock, loggingHelper) {
            @Override
            public String readLine() throws IOException {
                return "filename";
            }

        };


        SshRequestLog requestLog = Mockito.mock(SshRequestLog.class);
        NewScpCommand scpCommand = new NewScpCommand("scp -t " + filePath, requestLog) {
            @Override
            protected void initScpHelper() {
                this.helper = helperMocked;
                this.loggingHelper = Mockito.mock(LoggingHelper.class);
            }
        };

        scpCommand.setFileSystemView(viewMock);
        scpCommand.setOutputStream(osMock);
        scpCommand.setInputStream(isMock);
        scpCommand.setExitCallback(callback);

        return scpCommand;
    }


    @Test
    public void testFileFailure() throws IOException {
        ExitCallback callback = Mockito.mock(ExitCallback.class);
        NewScpCommand scpCommand = getFailureScpCommand((int) 'C', callback);
        scpCommand.run();

        Mockito.verify(callback).onExit(Matchers.anyInt(), Matchers.anyString());
    }

    @Test
    public void testDirFailure() throws IOException {
        ExitCallback callback = Mockito.mock(ExitCallback.class);
        NewScpCommand scpCommand = getFailureScpCommand((int) 'D', callback);
        scpCommand.run();

        Mockito.verify(callback).onExit(Matchers.anyInt(), Matchers.anyString());
    }

}
