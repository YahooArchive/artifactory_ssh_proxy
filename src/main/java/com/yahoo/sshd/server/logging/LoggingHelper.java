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

import javax.annotation.Nonnull;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.scp.ScpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.filesystem.ArtifactorySshFile;
import com.yahoo.sshd.server.logging.SshRequestInfo.Builder;
import com.yahoo.sshd.tools.artifactory.ArtifactoryExceptionInformation;
import com.yahoo.sshd.tools.artifactory.RepositoryAndPath;

public class LoggingHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHelper.class);
    private final Builder sshRequestInfo;
    private final SshRequestLogListener sshRequestLogListener;

    public LoggingHelper(final Builder sshRequestInfo, final SshRequestLogListener sshRequestLogListener) {
        this.sshRequestInfo = sshRequestInfo;
        this.sshRequestLogListener = sshRequestLogListener;
    }

    public void doLogging(@Nonnull final SshFile sshfile) {
        ArtifactorySshFile afSshFile = (ArtifactorySshFile) sshfile;
        this.sshRequestInfo.setPath(afSshFile.getAbsolutePath()).setSize(afSshFile.getSize())
                        .setRepoName(afSshFile.getRepoName());
        doLogging();
    }

    public void doLogging(@Nonnull final Throwable t, final String path) {
        int statusCode = SshRequestStatus.INTERNAL_SERVER_ERROR.getStatusCode();
        statusCode = getStatusCodeByThrowable(t);
        this.sshRequestInfo.setStatus(statusCode).setExitValue(ScpHelper.ERROR);
        SshFile sshFile = getSshFileFromException(t);

        if (null != sshFile) {
            doLogging(sshFile);
            return;
        }

        // we need to parse path
        RepositoryAndPath repositoryAndPath = RepositoryAndPath.splitRepositoryAndPath(path);
        this.sshRequestInfo.setPath(repositoryAndPath.getPath()).setRepoName(repositoryAndPath.getRepository());
        doLogging();
        return;
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
        if (e instanceof ArtifactoryExceptionInformation) {
            statusCode = ((ArtifactoryExceptionInformation) e).getStatusCode();
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

            statusCode = SshRequestStatus.INTERNAL_SERVER_ERROR.getStatusCode();

            if (null != errorMessage) {
                if (errorMessage.indexOf("Conflict") >= 0) {
                    statusCode = SshRequestStatus.CONFLICT.getStatusCode();
                } else if (errorMessage.indexOf("Method Not Allowed") >= 0) {
                    statusCode = SshRequestStatus.METHOD_NOT_ALLOWED.getStatusCode();
                } else if (errorMessage.indexOf("Illegal character in path") >= 0) {
                    statusCode = SshRequestStatus.BAD_REQUEST.getStatusCode();
                }
            }
        }
        return statusCode;
    }

    private static final SshFile getSshFileFromException(Throwable e) {
        if (e instanceof ArtifactoryExceptionInformation) {
            return ((ArtifactoryExceptionInformation) e).getFile();
        }
        return null;
    }

}
