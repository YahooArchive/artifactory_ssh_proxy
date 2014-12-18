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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;

public class LocalUserPKAuthenticator implements MultiUserPKAuthenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeDirectoryScanningPKAuthenticator.class);

    private final MultiUserAuthorizedKeysMap authorizedKeysMap;

    /**
     * Latch that countdown is called on when start is done registering everything.
     */
    protected final CountDownLatch wakeupLatch;

    private final String userHome;
    private final String userName;

    /**
     * 
     * @param countdownLatch the latch to countdown when it's done.
     * @param homeDirectoryBasePath Path to scan for user directories that contain .ssh/authorized_keys
     * @param excludedPaths Path's that while they live in /home, they should not be scanned. Some people put
     *        directories in /home that are not home directories and are rather large.
     * @throws IOException
     */
    public LocalUserPKAuthenticator(final CountDownLatch wakeupLatch) throws IOException {
        // Setup the scanner that keeps the pk's up to date.
        this.authorizedKeysMap = new MultiUserAuthorizedKeysMap();
        String home = System.getProperty("user.home");
        if (null == home) {
            throw new IOException("Unable to get home directory from user.home system property");
        }

        String username = System.getProperty("user.name");
        if (null == username) {
            throw new IOException("Unable to get username from user.name system property");
        }

        this.userHome = home;
        this.userName = username;
        this.wakeupLatch = wakeupLatch;
    }

    @Override
    public void start() throws IOException {
        try {
            File authorizedKeysFile = getAuthorizedKeysPath();
            authorizedKeysMap.updateUser(getUserName(),
                            AuthorizedKeysFileScanner.getStream(getUserName(), authorizedKeysFile));
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to load authorized_keys for " + userName, e);
        }
        wakeupLatch.countDown();
    }

    String getUserName() {
        return userName;
    }

    File getAuthorizedKeysPath() {
        return new File(getUserHome() + File.separator + ".ssh" + File.separator + "authorized_keys");
    }

    String getUserHome() {
        return userHome;
    }

    @Override
    public boolean authenticate(String username, PublicKey publicKey, ServerSession session) {
        return authorizedKeysMap.authenticate(username, publicKey, session);
    }

    @Override
    public Collection<String> getUsers() {
        return authorizedKeysMap.getUsers();
    }

    @Override
    public int getNumberOfKeysLoads() {
        return authorizedKeysMap.getUsers().size();
    }

}
