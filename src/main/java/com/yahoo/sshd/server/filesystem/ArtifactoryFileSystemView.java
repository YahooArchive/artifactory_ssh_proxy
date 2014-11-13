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
package com.yahoo.sshd.server.filesystem;

import java.io.IOException;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.file.nativefs.NativeSshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaDataParseFailureException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryClientFactory;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelper;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelperFactoryImpl;
import com.yahoo.sshd.tools.artifactory.RepositoryAndPath;

/**
 * Returns ArtifactorySshFile which will post/get files to/from artifactory using an artifactory client
 * 
 * @author areese
 * 
 */
public class ArtifactoryFileSystemView extends NativeFileSystemView {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryFileSystemView.class);

    protected final String sshUserName;
    protected final boolean caseInsensitive;
    protected final ArtifactoryInformation afInfo;
    protected final ArtifactoryClientFactory clientFactory;
    protected final ArtifactoryAuthorizer artifactoryAuthorizer;

    /**
     * @param afInfo
     * @param userName
     * @param caseInsensitive
     */
    public ArtifactoryFileSystemView(final ArtifactoryInformation afInfo, final String sshUserName,
                    final boolean caseInsensitive, final ArtifactoryAuthorizer artifactoryAuthorizer) {
        this(afInfo, sshUserName, caseInsensitive, new AFClientCache(afInfo,
                        new JFrogArtifactoryClientHelperFactoryImpl()), artifactoryAuthorizer);
    }

    /**
     * @param afInfo
     * @param userName
     * @param caseInsensitive
     * @param jFrogArtifactoryClientHelperFactoryImpl
     */
    public ArtifactoryFileSystemView(final ArtifactoryInformation afInfo, final String sshUserName,
                    final boolean caseInsensitive, final ArtifactoryClientFactory clientFactory,
                    final ArtifactoryAuthorizer artifactoryAuthorizer) {
        super(sshUserName, caseInsensitive);
        this.sshUserName = sshUserName;
        this.caseInsensitive = caseInsensitive;
        this.afInfo = afInfo;
        this.clientFactory = clientFactory;
        this.artifactoryAuthorizer = artifactoryAuthorizer;
    }

    @Override
    protected SshFile getFile(String dir, String file) {
        // get actual file object
        String physicalName = NativeSshFile.getPhysicalName("/", dir, file, caseInsensitive);

        // strip the root directory and the repository, then return

        RepositoryAndPath repositoryAndPath = RepositoryAndPath.splitRepositoryAndPath(physicalName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("user: " + repositoryAndPath + " phys: " + physicalName);
        }

        if (!isRepoWhitelisted(repositoryAndPath)) {
            LOGGER.info("user: {} attempted to write to non-whitelisted repo/path: {} ", repositoryAndPath);
            return null;
        }

        try {
            if (repositoryAndPath.isDevNull()) {
                return new DevNullSshFile(repositoryAndPath.getPath(), sshUserName, artifactoryAuthorizer);
            }

            return new ArtifactorySshFile(createJFrogClientHelper(repositoryAndPath), repositoryAndPath.getPath(),
                            sshUserName, repositoryAndPath.getRepository(), artifactoryAuthorizer);
        } catch (ArtifactMetaDataParseFailureException | IOException e) {
            throw new IllegalStateException("Could not get file " + file + ": " + e.getMessage(), e);
        }

    }

    protected JFrogArtifactoryClientHelper createJFrogClientHelper(final RepositoryAndPath repositoryAndPath) {
        return clientFactory.createJFrogClientHelper(afInfo, repositoryAndPath.getRepository());
    }

    protected boolean isRepoWhitelisted(RepositoryAndPath repositoryAndPath) {
        // FIXME: add repo whitelisting
        return true;
    }

    @Override
    public String getUserName() {
        return this.sshUserName;
    }
}
