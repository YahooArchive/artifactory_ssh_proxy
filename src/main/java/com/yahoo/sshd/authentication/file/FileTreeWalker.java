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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * an implementation of {@link FileTreeWalkerInterface} which looks for new public keys in
 * /<path>/<user>/.ssh/authorized_keys
 * 
 * @author areese
 * 
 */
public class FileTreeWalker implements FileTreeWalkerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTreeWalker.class);

    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    private final Map<Path, WatchKey> pathToWatchKeys = new HashMap<Path, WatchKey>();
    private final List<Path> excludedPaths;
    private final MultiUserAuthorizedKeysMap authorizedKeysMap;

    private final Path homeDirectoryBasePath;
    private final int homeDirNameCount;

    final class FileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

            dir = dir.toAbsolutePath();

            // skip anything not in /home
            if (!dir.startsWith(getHomeDirectoryBasePath())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Not visiting non-home dir {}", dir.toAbsolutePath());
                }
                return FileVisitResult.SKIP_SUBTREE;
            }
            // skip paths that are excluded, as we may not want to scan some directories for various reasons.
            for (Path excluded : getExcludedPaths()) {
                if (dir.startsWith(excluded)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Not visiting excluded dir {}", dir.toAbsolutePath());
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            // skip anything that is more than homedir length+2 and not
            // named .ssh
            final int dirCount = dir.getNameCount();
            if (dirCount > getHomeDirNameCount() + 1 && !dir.endsWith(".ssh")) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Not visiting deep dir {}", dir.toAbsolutePath());
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            register(dir);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Invoked for a file that could not be visited.
         * 
         * <p>
         * Unless overridden, this method re-throws the I/O exception that prevented the file from being visited.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to visit {}", file, exc);
            } else {
                LOGGER.info("Unable to visit {} because {}", file, exc.getMessage());
            }

            // keep going.
            return FileVisitResult.CONTINUE;
        }
    }

    private final FileVisitor fileVisitor = new FileVisitor();

    public FileTreeWalker(final WatchService watchService, final Map<WatchKey, Path> watchKeys,
                    final Path homeDirectoryBasePath, final List<Path> excludedPaths,
                    final MultiUserAuthorizedKeysMap authorizedKeysMap) {

        this.excludedPaths = new ArrayList<>(excludedPaths.size());

        // make sure all paths are absolute
        for (Path excluded : excludedPaths) {
            this.excludedPaths.add(excluded.toAbsolutePath());
        }

        this.watchService = watchService;
        this.watchKeys = watchKeys;
        this.authorizedKeysMap = authorizedKeysMap;
        this.homeDirectoryBasePath = homeDirectoryBasePath.toAbsolutePath();
        this.homeDirNameCount = this.homeDirectoryBasePath.getNameCount();
    }

    /**
     * Register the given directory with the WatchService
     */
    @Override
    public void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (LOGGER.isDebugEnabled()) {
            Path prev = watchKeys.get(key);
            if (prev == null) {
                LOGGER.debug("register: {} ", dir);
            } else {
                if (!dir.equals(prev)) {
                    LOGGER.debug("update: {} -> {}", prev, dir);
                }
            }
        }

        registerKey(dir, key);
    }

    void registerKey(Path dir, WatchKey key) throws FileNotFoundException {
        dir = dir.toAbsolutePath();
        File dirFile = dir.toFile();

        // we should only be getting directories here.
        if (!dirFile.isDirectory()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not adding file {}", dir.toAbsolutePath());
            }

            return;
        }

        final int dirLength = dir.getNameCount() - 1;
        if (!dir.startsWith(homeDirectoryBasePath) && dirLength != homeDirNameCount) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not adding non home directory {}", dir.toAbsolutePath());
            }

            return;
        } else if (dirLength <= homeDirNameCount) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding home directory {}", dir.toAbsolutePath());
            }

            addWatchKey(key, dir);
            return;
        } else if (dir.endsWith(".ssh")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding home/<user>/.ssh directory {}", dir.toAbsolutePath());
            }

            String userName = dir.getName(dir.getNameCount() - 2).toString();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("got user {} for {} ", userName, dirFile.getAbsolutePath());
            }

            authorizedKeysMap.updateUser(userName, AuthorizedKeysFileScanner.getStream(userName, new File(dirFile,
                            AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME)));

            addWatchKey(key, dir);

            // we need to remove the home dir now.
            removeWatchKey(dir.getParent());

            return;
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not adding non .ssh dir {}", dir.toAbsolutePath());
            }
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    @Override
    public void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, fileVisitor);
    }

    Map<WatchKey, Path> getWatchKeys() {
        return watchKeys;
    }

    List<Path> getExcludedPaths() {
        return excludedPaths;
    }

    Path getHomeDirectoryBasePath() {
        return homeDirectoryBasePath;
    }

    public int getHomeDirNameCount() {
        return homeDirNameCount;
    }

    private final void addWatchKey(final WatchKey key, final Path dir) {
        watchKeys.put(key, dir);
        pathToWatchKeys.put(dir, key);
    }

    private final void removeWatchKey(Path path) {
        WatchKey watchKey = pathToWatchKeys.get(path);
        if (null != watchKey) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing watch for {} ", path);
            }

            pathToWatchKeys.remove(path);
            watchKeys.remove(watchKey);
            watchKey.cancel();
        }
    }

}
