/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * - Neither the name of Oracle nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */

/*
 * Adapted from:
 * http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/essential
 * /io/examples/WatchDir.java
 */

package com.yahoo.sshd.authentication.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.DirectoryWatchService;
import com.yahoo.sshd.utils.DirectoryWatchServiceEventHandler;

public class AuthorizedKeysFileScanner extends DirectoryWatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizedKeysFileScanner.class);

    static final String AUTHORIZED_KEYS_NAME = "authorized_keys";
    static final String SSH_DIR_NAME = ".ssh";

    private final FileTreeWalkerInterface treeWalker;
    private final DirectoryWatchServiceEventHandler fileEventHandler;
    private final Map<WatchKey, Path> watchKeys = new HashMap<WatchKey, Path>();

    public AuthorizedKeysFileScanner(final CountDownLatch wakeupLatch,
                    final MultiUserAuthorizedKeysMap authorizedKeysMap, final File homeDirectoryBasePath,
                    final List<Path> excluded) throws IOException {
        super(wakeupLatch, homeDirectoryBasePath);

        LOGGER.info("Loading keys from " + homeDirectoryBasePath);
        this.treeWalker =
                        new FileTreeWalker(watchService, watchKeys, homeDirectoryBasePath.toPath(), excluded,
                                        authorizedKeysMap);
        this.fileEventHandler =
                        new FileBasedPKAuthenticatorEventHandler(watchedDirectory, authorizedKeysMap, this.treeWalker);
    }

    AuthorizedKeysFileScanner(final CountDownLatch wakeupLatch, final MultiUserAuthorizedKeysMap authorizedKeysMap,
                    final File homeDirectoryBasePath, final List<Path> excluded, FileTreeWalker treeWalker)
                    throws IOException {
        super(wakeupLatch, homeDirectoryBasePath);
        this.treeWalker = treeWalker;
        this.fileEventHandler =
                        new FileBasedPKAuthenticatorEventHandler(watchedDirectory, authorizedKeysMap, this.treeWalker);
    }

    @Override
    protected DirectoryWatchServiceEventHandler getFileEventHandler() {
        return fileEventHandler;
    }

    static final InputStream getStream(final String username, final File authorizedKeysFile)
                    throws FileNotFoundException {
        if (!authorizedKeysFile.exists() || !authorizedKeysFile.isFile() || !authorizedKeysFile.canRead()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("keys for " + username + " are not readable, not a file, or do not exist");
            }

            return null;
        }
        return new FileInputStream(authorizedKeysFile);
    }

    @Override
    protected Map<WatchKey, Path> getWatchKeys() {
        return this.watchKeys;
    }

    @Override
    protected FileTreeWalkerInterface getFileTreeWalker() {
        return this.treeWalker;
    }

    @Override
    protected void start() throws IOException {
        super.start();
    }
}
