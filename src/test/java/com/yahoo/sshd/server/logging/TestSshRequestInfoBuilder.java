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
package com.yahoo.sshd.server.logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.tools.artifactory.ArtifactMetaDataParseFailureException;
import com.yahoo.sshd.tools.artifactory.ArtifactNotFoundException;

public class TestSshRequestInfoBuilder {

    @Test
    public void testBuildSshRequestInfoObj() throws ArtifactNotFoundException, IOException,
                    ArtifactMetaDataParseFailureException, ParseException {
        IoSession ioSession = Mockito.mock(IoSession.class);
        Mockito.when(ioSession.getRemoteAddress()).thenReturn(new InetSocketAddress("10.0.0.1", 9999));
        ServerSession session = Mockito.mock(ServerSession.class);
        Mockito.when(session.getUsername()).thenReturn("screwdrv");
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        SshRequestInfo request =
                        new SshRequestInfo.Builder(session).setStartTimestamp(1411455384909L)
                                        .setMethod(SshRequestStatus.CREATED.getReasonPhrase())
                                        .setStatus(SshRequestStatus.CREATED.getStatusCode()).setExitValue(0)
                                        .setRepoName("maven-local-release").setPath("/com/yahoo/sshd/util/Utils.java")
                                        .setSize(1024000L).build();

        Assert.assertEquals(request.getStartTimestamp(), 1411455384909L);
        Assert.assertEquals(request.getRemoteAddr(), "10.0.0.1");
        Assert.assertEquals(request.getRepoName(), "maven-local-release");
        Assert.assertEquals(request.getRequestPath(), "/com/yahoo/sshd/util/Utils.java");
        Assert.assertEquals(request.getStatus(), 201);
        Assert.assertEquals(request.getExitValue(), 0);
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getUserName(), "screwdrv");
    }

    @Test
    public void testObjectEqual() throws ArtifactNotFoundException, IOException, ArtifactMetaDataParseFailureException,
                    ParseException {
        IoSession ioSession = Mockito.mock(IoSession.class);
        Mockito.when(ioSession.getRemoteAddress()).thenReturn(new InetSocketAddress("10.0.0.1", 9999));
        ServerSession session = Mockito.mock(ServerSession.class);
        Mockito.when(session.getUsername()).thenReturn("screwdrv");
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        SshRequestInfo request1 =
                        new SshRequestInfo.Builder(session).setStartTimestamp(1411455384909L)
                                        .setMethod(SshRequestStatus.CREATED.getReasonPhrase())
                                        .setStatus(SshRequestStatus.CREATED.getStatusCode()).setExitValue(0)
                                        .setRepoName("maven-local-release").setPath("/com/yahoo/sshd/util/Utils.java")
                                        .setSize(1024000L).build();

        SshRequestInfo request2 =
                        new SshRequestInfo.Builder(session).setStartTimestamp(1411455384909L)
                                        .setMethod(SshRequestStatus.OK.getReasonPhrase())
                                        .setStatus(SshRequestStatus.OK.getStatusCode()).setExitValue(0)
                                        .setRepoName("maven-local-release").setPath("/com/yahoo/sshd/util/Utils.java")
                                        .setSize(1024000L).build();

        Assert.assertFalse(request1.equals(request2));
    }
}
