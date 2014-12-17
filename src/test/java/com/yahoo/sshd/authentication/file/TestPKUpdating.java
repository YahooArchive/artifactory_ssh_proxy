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
import java.net.SocketAddress;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.AuthorizedKey;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestPKUpdating {
    private class TestContext {
        FileBasedPKAuthenticator publickeyAuthenticator;
    }

    static final String pubKeys = "src/test/resources/keys/";
    static final File pubKeysDir = new File(pubKeys);

    static final class User {
        final String name;
        final File publicKey;

        public User(String name, File publicKey) {
            this.name = name;
            this.publicKey = publicKey;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final User[] notUsers = new User[] { //
                    new User("y", new File(pubKeysDir, "test_ssh_key-0.pub")), //
                    };

    static final User[] normalUsers = new User[] {//
                    new User("areese", new File(pubKeysDir, "test_ssh_key-1.pub")), //
                                    new User("bob", new File(pubKeysDir, "test_ssh_key-2.pub")), //
                                    new User("sam", new File(pubKeysDir, "test_ssh_key-3.pub")), //
                    };

    static final User[] newUsers = new User[] {new User("ted", new File(pubKeysDir, "test_ssh_key-4.pub")), //
                    new User("novak", new File(pubKeysDir, "test_ssh_key-5.pub")), //
                    new User("teddy", new File(pubKeysDir, "test_ssh_key-6.pub")), //
    };

    static final String home = "target/test/walking_test/home/";
    static final File homeDir = new File(home);

    private TestContext setup() throws IOException, InterruptedException {
        TestContext testContext = new TestContext();

        // first build dirs.

        if (homeDir.exists()) {
            FileUtils.forceDelete(homeDir);
        }

        FileUtils.forceMkdir(homeDir);

        // build user directories.
        User[] dirs = new User[normalUsers.length + notUsers.length];
        System.arraycopy(normalUsers, 0, dirs, 0, normalUsers.length);
        System.arraycopy(notUsers, 0, dirs, normalUsers.length, notUsers.length);

        buildSshDirs(homeDir, dirs);

        // this point everyone in users has a public key, so we can load it, and
        // check for them.
        CountDownLatch waiter = new CountDownLatch(1);
        testContext.publickeyAuthenticator =
                        new FileBasedPKAuthenticator(waiter, homeDir, Arrays.asList(new Path[] {new File(homeDir, "y")
                                        .toPath()}));
        testContext.publickeyAuthenticator.start();

        waiter.await();

        checkExist(testContext, normalUsers);
        checkDoesntExist(testContext, notUsers);

        return testContext;
    }

    private void buildSshDirs(File homeDir, User[] dirs) throws IOException {
        for (User user : dirs) {
            File userDir = new File(homeDir, user.name);
            File sshDir = new File(userDir, ".ssh");
            File authKeys = new File(sshDir, "authorized_keys");

            FileUtils.forceMkdir(sshDir);

            // give them public keys
            FileUtils.copyFile(user.publicKey, authKeys);
        }
    }

    // FIXME: fails on OSX but not cygwin or rhel
    @Test(enabled = false)
    public void testUpdating() throws IOException, InterruptedException, NoSuchAlgorithmException,
                    InvalidKeySpecException {
        TestContext testContext = setup();

        // at this point, all of the dummy users are in place
        // now we add some new users and check for them.

        // check they don't exist.
        checkDoesntExist(testContext, newUsers);

        // create them
        buildSshDirs(homeDir, newUsers);

        // Wait for it.
        TimeUnit.SECONDS.sleep(10);

        // check that they exist.
        checkExist(testContext, newUsers);

        // first see if we can auth them.
        User user = normalUsers[0];
        try (FileInputStream fis = new FileInputStream(user.publicKey)) {
            Map<PublicKey, AuthorizedKey> parseAuthorizedKeys = KarafPublickeyAuthenticator.parseAuthorizedKeys(fis);

            ServerSession sessionMock = Mockito.mock(ServerSession.class);
            IoSession ioSessionMock = Mockito.mock(IoSession.class);
            SocketAddress socketMock = Mockito.mock(SocketAddress.class);

            Mockito.when(sessionMock.getIoSession()).thenReturn(ioSessionMock);
            Mockito.when(ioSessionMock.getRemoteAddress()).thenReturn(socketMock);

            testContext.publickeyAuthenticator.authenticate(normalUsers[0].name, parseAuthorizedKeys.keySet()
                            .iterator().next(), sessionMock);
        }

        // now overwrite some keys and see that it works.
        // the first users are 0-5
        // the second set are 10-15

        //
    }

    private void checkExist(TestContext testContext, User[] users) {
        Collection<String> authenticatedUsers = testContext.publickeyAuthenticator.getUsers();

        for (User user : users) {
            Assert.assertTrue(authenticatedUsers.contains(user.name), "expected " + user + " to be authenticated");
        }
    }

    private void checkDoesntExist(TestContext testContext, User[] users) {
        Collection<String> authenticatedUsers = testContext.publickeyAuthenticator.getUsers();

        for (User user : users) {
            Assert.assertFalse(authenticatedUsers.contains(user.name), "expected " + user + " not to be authenticated");
        }
    }
}
