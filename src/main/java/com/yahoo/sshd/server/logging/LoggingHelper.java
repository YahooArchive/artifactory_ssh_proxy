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
package com.yahoo.sshd.server.logging;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.scp.ScpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.filesystem.ArtifactorySshFile;
import com.yahoo.sshd.server.logging.SshRequestInfo.Builder;
import com.yahoo.sshd.tools.artifactory.ArtifactoryFileNotFoundException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryNoReadPermissionException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryNoWritePermissionException;
import com.yahoo.sshd.tools.artifactory.RepositoryAndPath;

public class LoggingHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHelper.class);
    private final Builder sshRequestInfo;
    private final SshRequestLogListener sshRequestLogListener;

    public LoggingHelper(final Builder sshRequestInfo, final SshRequestLogListener sshRequestLogListener) {
        this.sshRequestInfo = sshRequestInfo;
        this.sshRequestLogListener = sshRequestLogListener;
    }

    public void doLogging(final SshFile sshfile) {
        if (null == sshfile) {
            throw new IllegalArgumentException("logging expects SshFile as an argument");
        }
        ArtifactorySshFile afSshFile = (ArtifactorySshFile) sshfile;
        this.sshRequestInfo.setPath(afSshFile.getAbsolutePath()).setSize(afSshFile.getSize())
                        .setRepoName(afSshFile.getRepoName());
        doLogging();
    }

    public void doLogging(final Throwable t, final String path) {
        if (null == t) {
            throw new IllegalArgumentException("logging expects Throwable as an argument");
        }
        int statusCode = SshRequestStatus.INTERNAL_SERVER_ERROR.getStatusCode();
        SshFile sshFile = null;
        statusCode = getStatusCodeByThrowable(t);
        this.sshRequestInfo.setStatus(statusCode).setExitValue(ScpHelper.ERROR);
        sshFile = getSshFileFromException(t);
        if (null == sshFile) {
            // we need to parse path
            RepositoryAndPath repositoryAndPath = RepositoryAndPath.splitRepositoryAndPath(path);
            this.sshRequestInfo.setPath(repositoryAndPath.getPath()).setRepoName(repositoryAndPath.getRepository());
            doLogging();
            return;
        }
        doLogging(sshFile);
    }

    private void doLogging() {
        if (null == this.sshRequestLogListener) {
            LOGGER.warn("requestLogListener is not initialized, no request logging");
            return;
        }
        sshRequestLogListener.handleRequest(this.sshRequestInfo.build());
    }

    private static final int getStatusCodeByThrowable(Throwable e) {
        int statusCode;
        if (e instanceof ArtifactoryFileNotFoundException) {
            statusCode = SshRequestStatus.NOT_FOUND.getStatusCode();
        } else if (e instanceof ArtifactoryNoReadPermissionException
                        || e instanceof ArtifactoryNoWritePermissionException) {
            statusCode = SshRequestStatus.FORBIDDEN.getStatusCode();
        } else if (e.getCause() instanceof org.apache.http.client.HttpResponseException) {
            statusCode = ((org.apache.http.client.HttpResponseException) e.getCause()).getStatusCode();
        } else {
            // there can be other IOExceptions talking to artifactory
            // last try to distinguish them by parsing error message
            // default to 400 instead of 500 as they are all IOExceptions
            String errorMessage = e.getMessage();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("generic IOException, errorMessage: " + errorMessage);
            }
            if (errorMessage.indexOf("Conflict") >= 0) {
                statusCode = SshRequestStatus.CONFLICT.getStatusCode();
            } else {
                statusCode = SshRequestStatus.INTERNAL_SERVER_ERROR.getStatusCode();
            }
        }
        return statusCode;
    }

    private static final SshFile getSshFileFromException(Throwable e) {
        if (e instanceof ArtifactoryNoReadPermissionException) {
            return ((ArtifactoryNoReadPermissionException) e).getFile();
        } else if (e instanceof ArtifactoryFileNotFoundException) {
            return ((ArtifactoryFileNotFoundException) e).getFile();
        } else if (e instanceof ArtifactoryNoWritePermissionException) {
            return ((ArtifactoryNoWritePermissionException) e).getFile();
        } else {
            return null;
        }
    }

}
