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

package com.yahoo.sshd.authorization.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.yahoo.sshd.authentication.file.FileTreeWalkerInterface;
import com.yahoo.sshd.utils.DirectoryWatchService;
import com.yahoo.sshd.utils.DirectoryWatchServiceEventHandler;

public class ArtifactoryAuthorizerFileScanner extends DirectoryWatchService {
    private final ArtifactoryAuthFileEventHandler artifactoryAuthFileEventHandler;
    private final Map<WatchKey, Path> watchKeys = new HashMap<WatchKey, Path>();
    private final FileTreeWalkerInterface treeWalker;

    public ArtifactoryAuthorizerFileScanner(final CountDownLatch wakeupLatch, final File watchedDirectory,
                    final ConcurrentHashMap<String, PermTarget> authorizationHashMap) throws IOException {
        super(wakeupLatch, watchedDirectory.getParentFile());
        this.artifactoryAuthFileEventHandler =
                        new ArtifactoryAuthFileEventHandler(authorizationHashMap, watchedDirectory);
        this.treeWalker =
                        new ArtifactoryAuthFileWalker(watchService, watchKeys, watchedDirectory, authorizationHashMap);
    }

    @Override
    protected DirectoryWatchServiceEventHandler getFileEventHandler() {
        return this.artifactoryAuthFileEventHandler;
    }

    @Override
    protected Map<WatchKey, Path> getWatchKeys() {
        return this.watchKeys;
    }

    @Override
    protected FileTreeWalkerInterface getFileTreeWalker() {
        return this.treeWalker;
    }

}
