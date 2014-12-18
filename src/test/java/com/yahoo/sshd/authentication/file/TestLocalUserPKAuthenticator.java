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
package com.yahoo.sshd.authentication.file;

import java.io.FileInputStream;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.AuthorizedKey;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class TestLocalUserPKAuthenticator {
    @Test
    public void testDefault() throws Exception {
        CountDownLatch wakeupLatch = new CountDownLatch(1);
        LocalUserPKAuthenticator lpka = new LocalUserPKAuthenticator(wakeupLatch) {

            @Override
            String getUserName() {
                return "areese";
            }

            @Override
            String getUserHome() {
                return "src/test/resources/MultiUserPKAuthenticator/home/areese/";
            }
        };

        lpka.start();

        ServerSession session = Mockito.mock(ServerSession.class);
        IoSession ioSession = Mockito.mock(IoSession.class);
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        Map<PublicKey, AuthorizedKey> publicKeys = null;
        try (FileInputStream karafFis = new FileInputStream(lpka.getAuthorizedKeysPath())) {
            publicKeys = KarafPublickeyAuthenticator.parseAuthorizedKeys(karafFis);
        }

        for (PublicKey publicKey : publicKeys.keySet()) {
            lpka.authenticate("areese", publicKey, session);
        }

    }
}
