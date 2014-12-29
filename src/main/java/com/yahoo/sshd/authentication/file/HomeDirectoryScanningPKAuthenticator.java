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
import java.io.InputStream;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.MultiUserPKAuthenticator;
import com.yahoo.sshd.utils.ThreadUtils;

/**
 * A public key authenticator that scans /home/<user>/.ssh/authorized_keys for public key files.
 * 
 * @author areese
 * 
 */
public class HomeDirectoryScanningPKAuthenticator implements MultiUserPKAuthenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeDirectoryScanningPKAuthenticator.class);
    private final AuthorizedKeysFileScanner authorizedKeysFileScanner;

    private final MultiUserAuthorizedKeysMap authorizedKeysMap;

    /**
     * 
     * @param countdownLatch the latch to countdown when it's done.
     * @param homeDirectoryBasePath Path to scan for user directories that contain .ssh/authorized_keys
     * @param excludedPaths Path's that while they live in /home, they should not be scanned. Some people put
     *        directories in /home that are not home directories and are rather large.
     * @throws IOException
     */
    public HomeDirectoryScanningPKAuthenticator(final CountDownLatch wakeupLatch, final File homeDirectoryBasePath,
                    final List<Path> excludedPaths) throws IOException {
        // Setup the scanner that keeps the pk's up to date.
        this.authorizedKeysMap = new MultiUserAuthorizedKeysMap();
        this.authorizedKeysFileScanner =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, homeDirectoryBasePath,
                                        excludedPaths);
    }

    /**
     * Starts the {@link AuthorizedKeysFileScanner} thread.
     * 
     * @throws IOException
     */
    @Override
    public void start() throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting akfs thread.");
        }

        ThreadUtils.cachedThreadPool().execute(authorizedKeysFileScanner);
    }

    /**
     * given a user name and a file, update their authorized keys. Only used by one of the tests.
     * 
     * @param username
     * @param authorizedKeysFile
     * @throws FileNotFoundException
     */
    public void updateUser(final String username, final String filename, final InputStream authorizedKeysFile)
                    throws FileNotFoundException {
        authorizedKeysMap.updateUser(username, filename, authorizedKeysFile);
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
