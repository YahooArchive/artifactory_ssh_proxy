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

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.DirectoryWatchServiceEventHandler;

public class HomeDirectoryScanningPKEventHandler implements DirectoryWatchServiceEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeDirectoryScanningPKEventHandler.class);

    private final Path watchedDirectory;
    private final MultiUserAuthorizedKeysMap authorizedKeysMap;
    private final FileTreeWalkerInterface treeWalker;

    public HomeDirectoryScanningPKEventHandler(final Path homeDirectoryBasePath,
                    final MultiUserAuthorizedKeysMap authorizedKeysMap, final FileTreeWalkerInterface treeWalker) {
        this.watchedDirectory = homeDirectoryBasePath.toAbsolutePath();
        this.authorizedKeysMap = authorizedKeysMap;
        this.treeWalker = treeWalker;
    }

    @Override
    public void onProcessEvents(Path pathThatChanged) {
        try {
            handleWatchedPathChanged(pathThatChanged);
        } catch (FileNotFoundException e) {
            LOGGER.error("error with file {}", pathThatChanged, e);
        }

    }

    void handleWatchedPathChanged(Path pathThatChanged) throws FileNotFoundException {

        if (pathThatChanged.endsWith(AuthorizedKeysFileScanner.SSH_DIR_NAME)) {
            // if we get the .ssh dir, then process the file.
            handleWatchedPathChanged(new File(pathThatChanged.toFile(), AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME)
                            .toPath());

            return;
        }

        if (!pathThatChanged.endsWith(AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME)) {
            // if it's not an authorized_keys file we don't care.
            return;
        }

        // first we need to resolve it against the directory we are using.
        Path resolvedPath = pathThatChanged.toAbsolutePath();

        // this should be a file
        File authorizedKeysFile = resolvedPath.toFile();

        if (!authorizedKeysFile.isFile()) {
            throw new FileNotFoundException(authorizedKeysFile.getAbsolutePath());
        }

        // if we have a file we want to read it as an authorized key for the
        // user it belongs to.
        // we are watching /home/, so we want /home/<user>/.ssh/authorized_keys
        // as the file.
        // so we'll relativize against /home, which is what watchedDirectory is.
        Path userPath = watchedDirectory.relativize(resolvedPath);

        // now we need to see if it's long enough, it's 3 because
        // <user>/.ssh/authorized_keys
        if (3 != userPath.getNameCount()) {
            LOGGER.info("Error userpath too long {} : {}", Integer.valueOf(userPath.getNameCount()), userPath);
            return;
        }

        // the user is 0, .ssh is 1, and authorized_keys is 2.
        String username = userPath.getName(0).toString();
        // now we have a complete path (file), and a username, so we can load the key
        loadAuthorizedKeysForUser(username, authorizedKeysFile);
    }

    public final void loadAuthorizedKeysForUser(String username, File authorizedKeysFile) {
        // This should just push onto a queue and deal with it there.
        // we expect a large number of changes at once.
        // for now we'll do it inline. this exists just to allow that later.
        // also final as a hint to the compiler to eliminate this method.
        try {
            if (null != authorizedKeysMap) {
                authorizedKeysMap.updateUser(username,
                                AuthorizedKeysFileScanner.getStream(username, authorizedKeysFile));
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to load authorized_keys for " + username, e);
        }
    }

    @Override
    public void onProcessCreateEvents(Path pathThatChanged) {
        if (Files.isDirectory(pathThatChanged, NOFOLLOW_LINKS)) {
            try {
                treeWalker.registerAll(pathThatChanged);
            } catch (IOException e) {
                LOGGER.error("error with directory {}", pathThatChanged, e);
            }
        } else {
            // it wasn't a directory created, so we'll hand it
            // off in case they created an auth keys file.
            // we have a path, so lets do something about it.
            // handleWatchedPathChanged(child);
            onProcessEvents(pathThatChanged);
        }
    }

}
