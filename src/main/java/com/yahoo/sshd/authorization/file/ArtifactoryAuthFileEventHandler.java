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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.DirectoryWatchService;
import com.yahoo.sshd.utils.DirectoryWatchServiceEventHandler;

/**
 * This class is used by {@link ArtifactoryAuthorizerFileScanner} to deal with events fired by the
 * {@link DirectoryWatchService} implemented by {@link ArtifactoryAuthorizerFileScanner}
 * 
 * It hands parsing off to {@link AuthFileParser} which currently isn't replacable.
 * 
 * @author areese
 * 
 */
public class ArtifactoryAuthFileEventHandler implements DirectoryWatchServiceEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryAuthFileEventHandler.class);

    private final ConcurrentHashMap<String, PermTarget> authorizationHashMap;
    private final File authFile;

    public ArtifactoryAuthFileEventHandler(final ConcurrentHashMap<String, PermTarget> authorizationHashMap,
                    final File authFile) {
        this.authorizationHashMap = authorizationHashMap;
        this.authFile = authFile;
    }

    @Override
    public void onProcessEvents(Path changed) {
        if (changed.toFile().getAbsolutePath().equals(authFile.getAbsolutePath())) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("file {} changed.  reloading auth file...", changed);
                }
                reload(changed);
            } catch (FileNotFoundException e) {
                LOGGER.error("error with file {}", changed, e);
            }
        }
    }

    @Override
    public void onProcessCreateEvents(Path changed) {
        if (changed.toFile().getAbsolutePath().equals(authFile.getAbsolutePath())) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("file {} created.  reloading auth file...", changed);
                }
                reload(changed);
            } catch (FileNotFoundException e) {
                LOGGER.error("error with file {}", changed, e);
            }
        }
    }

    private void reload(Path changed) throws FileNotFoundException {
        // TODO: allow this to be overriden
        // TODO: fix this to not be static, so behaviour can change
        AuthFileParser.parse(changed.toFile().getAbsolutePath(), authorizationHashMap);
    }
}
