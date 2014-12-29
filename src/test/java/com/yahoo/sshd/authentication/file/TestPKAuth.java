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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.AuthorizedKey;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.DirectoryScanner;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;

@Test(groups = "unit")
public class TestPKAuth {
    HomeDirectoryScanningPKAuthenticator mupka;
    String[] pkFiles;
    String base = "src/test/resources/keys";
    DirectoryScanner ds = new DirectoryScanner(base, "**.pub");
    Map<String, Map<PublicKey, AuthorizedKey>> fileToPkMap = new HashMap<>();
    List<Path> excludedPaths = Arrays.asList(new Path[] {new File("/home/excluded").toPath()});

    public TestPKAuth() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        mupka = new HomeDirectoryScanningPKAuthenticator(new CountDownLatch(0), new File(base), excludedPaths);

        pkFiles = ds.scan();

        for (String fileName : pkFiles) {
            try (FileInputStream fis = new FileInputStream(new File(base + File.separator + fileName))) {
                mupka.updateUser(fileName, fileName, fis);
                try (FileInputStream karafFis = new FileInputStream(base + File.separator + fileName)) {
                    fileToPkMap.put(fileName, KarafPublickeyAuthenticator.parseAuthorizedKeys(fileName, karafFis));
                }
            }
        }
    }

    @DataProvider
    public Object[][] matchedKeys() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        List<Object[]> ret = new ArrayList<>();

        for (String fileName : pkFiles) {
            Object[] params = new Object[4];
            params[0] = mupka;
            params[1] = new ArrayList<>(fileToPkMap.get(fileName).keySet()).get(0);
            // we use filename as the user.
            params[2] = fileName;
            params[3] = fileName;

            ret.add(params);
        }

        return ret.toArray(new Object[][] {});
    }

    @DataProvider
    public Object[][] misMatchedKeys() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        List<Object[]> ret = new ArrayList<>();

        List<PublicKey> publicKeys = new ArrayList<>();

        for (String fileName : pkFiles) {
            publicKeys.add(new ArrayList<>(fileToPkMap.get(fileName).keySet()).get(0));
        }

        for (int i = 0; i < pkFiles.length; i++) {
            int r = i + 1;
            if (r >= pkFiles.length)
                r = 0;

            Assert.assertNotEquals(pkFiles[r], pkFiles[i]);

            Object[] params = new Object[4];
            params[0] = mupka;
            params[1] = publicKeys.get(r);
            // we use filename as the user.
            params[2] = pkFiles[i];
            params[3] = pkFiles[i];

            ret.add(params);
        }

        return ret.toArray(new Object[][] {});
    }

    @Test(dataProvider = "matchedKeys")
    public void testAllKeys(MultiUserPKAuthenticator mupka, PublicKey publicKey, String user, String fileName)
                    throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        // now for all of these public keys ensure that "test", auths against them
        ServerSession session = Mockito.mock(ServerSession.class);
        IoSession ioSession = Mockito.mock(IoSession.class);
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        Assert.assertTrue(mupka.authenticate(user, publicKey, session), fileName);
        Assert.assertFalse(mupka.authenticate("areese", publicKey, session), fileName);
    }

    @Test(dataProvider = "misMatchedKeys")
    public void testBadKeys(MultiUserPKAuthenticator mupka, PublicKey publicKey, String user, String fileName)
                    throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        // now for all of these public keys ensure that "test", can auth against them
        ServerSession session = Mockito.mock(ServerSession.class);
        IoSession ioSession = Mockito.mock(IoSession.class);
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        Assert.assertFalse(mupka.authenticate(user, publicKey, session), fileName);
        Assert.assertFalse(mupka.authenticate("areese", publicKey, session), fileName);
    }

}
