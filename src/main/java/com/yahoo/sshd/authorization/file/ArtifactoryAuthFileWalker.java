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
package com.yahoo.sshd.authorization.file;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authentication.file.FileTreeWalkerInterface;

public class ArtifactoryAuthFileWalker implements FileTreeWalkerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryAuthFileWalker.class);

    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    private final File watchedDirectory;
    private final ConcurrentHashMap<String, PermTarget> authorizationHashMap;

    public ArtifactoryAuthFileWalker(final WatchService watchService, final Map<WatchKey, Path> watchKeys,
                    final File watchedDirectory, ConcurrentHashMap<String, PermTarget> authorizationHashMap) {
        this.watchService = watchService;
        this.watchKeys = watchKeys;
        this.watchedDirectory = watchedDirectory;
        this.authorizationHashMap = authorizationHashMap;
    }

    @Override
    public void registerAll(Path dir) throws IOException {
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

        watchKeys.put(key, dir);
        AuthFileParser.parse(watchedDirectory.getAbsolutePath(), authorizationHashMap);
    }

    @Override
    public void register(Path dir) throws IOException {

    }

}
