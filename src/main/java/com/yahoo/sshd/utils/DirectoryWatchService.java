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

package com.yahoo.sshd.utils;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.file.FileTreeWalkerInterface;

public abstract class DirectoryWatchService implements AutoCloseable, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryWatchService.class);

    protected final Path watchedDirectory;
    protected final WatchService watchService;
    protected final CountDownLatch wakeupLatch;

    public DirectoryWatchService(final CountDownLatch wakeupLatch, final File watchedDirectory) throws IOException {
        if (!watchedDirectory.exists() || !watchedDirectory.isDirectory() || !watchedDirectory.canRead()) {
            throw new FileNotFoundException(watchedDirectory.getAbsolutePath());
        }

        this.wakeupLatch = wakeupLatch;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchedDirectory = watchedDirectory.toPath().toAbsolutePath();
    }

    /**
     * Implement event handler.
     * 
     * @return
     */
    protected abstract DirectoryWatchServiceEventHandler getFileEventHandler();

    /**
     * 
     * @return
     */
    protected abstract FileTreeWalkerInterface getFileTreeWalker();

    /**
     * 
     * @return
     */
    protected abstract Map<WatchKey, Path> getWatchKeys();

    /**
     * Register the tree of directories to monitor
     * 
     * @throws IOException
     */
    protected void start() throws IOException {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scanning {} ...\n", this.watchedDirectory);
            }

            getFileTreeWalker().registerAll(this.watchedDirectory);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Done.");
            }
        } finally {
            if (null != wakeupLatch) {
                wakeupLatch.countDown();
            }
        }
    }


    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            // check if it's an inotify exception and count what we've seen.
            if (e.getMessage() != null && e.getMessage().contains("inotify")) {
                LOGGER.error("exceeded inotify watches, with {} entries", Integer.valueOf(getWatchKeys().size()));
            } else {
                LOGGER.error("Failed to scan directories", e);
                throw new RuntimeException(e);
            }
        }
        processEvents();
    }

    @Override
    public void close() {
        try {
            watchService.close();
        } catch (IOException e) {
            LOGGER.error("close failed", e);
        }
    }

    void processEvents() {
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watchService.take();

                if (!key.isValid()) {
                    // if it's no longer valid, remove the key.
                    getWatchKeys().remove(key);
                }

            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                return;
            }

            Path dir = getWatchKeys().get(key);
            if (dir == null) {
                // this just means we got an event we didn't register for.
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("WatchKey not recognized: {}", key);
                }
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    LOGGER.info("Overflow encountered, auth key changes were dropped.");
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (kind == ENTRY_CREATE) {

                    try {
                        // not sure we care about this.
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            getFileTreeWalker().registerAll(child);
                        } else {
                            // it wasn't a directory created, so we'll hand it
                            // off in case they created an auth keys file.
                            // we have a path, so lets do something about it.
                            this.getFileEventHandler().onProcessCreateEvents(child);
                        }
                    } catch (IOException x) {
                        LOGGER.error("error with directory {}", child, x);
                    }
                } else {
                    // we have a path, so lets do something about it.
                    // handleWatchedPathChanged(child);
                    this.getFileEventHandler().onProcessEvents(child);
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                getWatchKeys().remove(key);

                // all directories are inaccessible
                if (getWatchKeys().isEmpty()) {
                    LOGGER.error("Unable to continue watching " + watchedDirectory);
                    break;
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

}
