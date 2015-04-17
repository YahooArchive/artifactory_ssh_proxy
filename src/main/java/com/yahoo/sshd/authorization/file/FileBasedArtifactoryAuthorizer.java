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
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.authorization.ArtifactoryPermTargetType;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;
import com.yahoo.sshd.utils.ThreadUtils;

/**
 * This class watches the file specified by {@link SshdSettingsInterface#getArtifactoryAuthorizationFilePath()} and
 * authorizes users based upon it's content.
 * 
 * @author areese
 * 
 */
public class FileBasedArtifactoryAuthorizer implements ArtifactoryAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedArtifactoryAuthorizer.class);
    private static final int DEFAULT_WAIT_TIMEOUT_SECS = 120;
    private final ConcurrentHashMap<String, PermTarget> authorizationHashMap;

    public FileBasedArtifactoryAuthorizer(final SshdSettingsInterface settings) {
        try {
            // Setup the scanner that keeps the auth file up to date.
            this.authorizationHashMap = new ConcurrentHashMap<String, PermTarget>();

            final File watchDirectory = new File(settings.getArtifactoryAuthorizationFilePath());

            if (!watchDirectory.exists()) {
                LOG.warn("Unable to find authorization file {}", watchDirectory);
            }

            // Make sure we sleep until this is ready
            final CountDownLatch countdownLatch = new CountDownLatch(1);
            try (ArtifactoryAuthorizerFileScanner artifactoryAuthorizerFileScanner =
                            new ArtifactoryAuthorizerFileScanner(countdownLatch, watchDirectory, authorizationHashMap)) {

                ThreadUtils.cachedThreadPool().execute(artifactoryAuthorizerFileScanner);
                LOG.info("Waiting for authorization file to be loaded with timeout {} seconds", DEFAULT_WAIT_TIMEOUT_SECS);
                countdownLatch.await(DEFAULT_WAIT_TIMEOUT_SECS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Done waiting for authorization file to be loaded");
    }

    @Override
    public boolean authorized(String repositoryName, String userName, ArtifactoryPermTargetType permissionTarget) {
        final PermTarget permTarget = authorizationHashMap.get(repositoryName);

        // fail fast if repository is not found.
        if (permTarget == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Repository {} not found in auth.txt", repositoryName);
            }
            return false;
        }
        if (permissionTarget.equals(ArtifactoryPermTargetType.READ)) {
            // check read perm
            return permTarget.canRead(userName);
        } else if (permissionTarget.equals(ArtifactoryPermTargetType.WRITE)) {
            // check write perm
            return permTarget.canWrite(userName);
        } else {
            return false;
        }
    }
    
    public ConcurrentHashMap<String, PermTarget> getAuthorizationHashMap() {
        return authorizationHashMap;
    }

}
