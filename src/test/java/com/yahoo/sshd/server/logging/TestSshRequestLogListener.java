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

import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class TestSshRequestLogListener {

    @SuppressWarnings("resource")
    @Test
    public void testRequestLogDoLog() {
        ServerSession session = Mockito.mock(ServerSession.class);
        SshRequestInfo requestInfo = Mockito.mock(SshRequestInfo.class);
        SshRequestLog requestLogger = Mockito.mock(SshRequestLog.class);

        SshRequestLogListener logListener = new SshRequestLogListener(requestLogger);
        logListener.handleRequest(requestInfo);
        logListener.sessionClosed(session);
        Mockito.verify(requestLogger).log(requestInfo);
    }
}
