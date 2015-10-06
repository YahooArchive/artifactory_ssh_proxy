/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */
package com.yahoo.sshd.server.command;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sshd.common.file.FileSystemAware;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.scp.ScpHelper;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.command.ScpCommand;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.logging.LoggingHelper;
import com.yahoo.sshd.server.logging.SshRequestInfo;
import com.yahoo.sshd.server.logging.SshRequestLog;
import com.yahoo.sshd.server.logging.SshRequestLogListener;
import com.yahoo.sshd.server.logging.SshRequestStatus;

/**
 * This commands provide SCP support on both server and client side. Permissions and preservation of access /
 * modification times on files are not supported.
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */

/**
 * This was adapted directly from the ScpCommand class in Apache Mina, so leaving license and original comment in place.
 *
 * This exists to allow use to replace parts of ScpCommand.
 */

public abstract class AbstractScpCommand extends ScpCommand implements Command, Runnable, FileSystemAware, SessionAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScpCommand.class);
    protected ServerSession session;
    protected SshRequestLogListener requestLogListener;
    protected ScpCommandSessionListener scpCommandSessionListener;
    protected Environment env;
    protected Map<String, String> envToAfPropertyMapping;

    public AbstractScpCommand(@Nonnull final String command, @Nonnull Map<String, String> envToAfPropertyMapping) {
        super(command);
        this.envToAfPropertyMapping = envToAfPropertyMapping;
    }

    protected NewScpHelper createScpHelper() {
        SshRequestInfo.Builder sshRequestInfo =
                        new SshRequestInfo.Builder(this.session).setStartTimestamp(System.currentTimeMillis());
        if (optT) {
            sshRequestInfo.setMethod(SshRequestStatus.CREATED.getReasonPhrase()).setStatus(
                            SshRequestStatus.CREATED.getStatusCode());
        } else if (optF) {
            sshRequestInfo.setMethod(SshRequestStatus.OK.getReasonPhrase()).setStatus(
                            SshRequestStatus.OK.getStatusCode());
        } else {
            sshRequestInfo.setMethod(SshRequestStatus.BAD_REQUEST.getReasonPhrase()).setStatus(
                            SshRequestStatus.BAD_REQUEST.getStatusCode());
        }
        LoggingHelper loggingHelper = new LoggingHelper(sshRequestInfo, requestLogListener);

        return new NewScpHelper(in, out, root, loggingHelper, env, envToAfPropertyMapping);
    }

    @Override
    public void start(Environment env) throws IOException {
        // save the env.
        this.env = env;
        super.start(env);
    }

    @Override
    public void run() {
        int exitValue = ScpHelper.OK;
        String exitMessage = null;
        // ScpHelper helper = new ScpHelper(in, out, root);
        NewScpHelper helper = createScpHelper();
        SshFile sshFile = null;
        registerScpCommandSessionListener(this.session);

        try {
            if (optT) { // upload
                sshFile = root.getFile(path);
                if (null == sshFile) {
                    throw new IOException("Unable to get path for " + path);
                }
                helper.receive(sshFile, optR, optD, optP);
            } else if (optF) { // download
                helper.send(Collections.singletonList(path), optR, optP);
            } else {
                throw new IOException("Unsupported mode");
            }
        } catch (Throwable e) {
            exitValue = ScpHelper.ERROR;
            exitMessage = e.getMessage() == null ? "" : e.getMessage();
            // out.write(exitValue);
            // out.write(exitMessage.getBytes());
            // out.write('\n');
            // out.flush();
            writeBackErrorMessage(out, exitValue, exitMessage);
            helper.doLogging(e, path);
            LOGGER.info("Error in scp command", e);
        } finally {
            if (null != sshFile) {
                try {
                    sshFile.handleClose();
                } catch (IOException e) {
                    LOGGER.error("Error when closing the SshFile: {}", sshFile.getName(), e);
                }
            }

            if (callback != null) {
                callback.onExit(exitValue, exitMessage);
            }
        }
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = session;
        @SuppressWarnings("resource")
        SshRequestLog requestLog = getSshRequestLog();
        if (null == requestLog) {
            return;
        }

        requestLogListener = new SshRequestLogListener(requestLog);
        requestLogListener.registerSession(session);
    }

    private void writeBackErrorMessage(@Nonnull OutputStream out, int exitValue, @Nonnull String exitMessage) {
        try {
            out.write(exitValue);
            out.write(exitMessage.getBytes());
            out.write('\n');
            out.flush();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("flush failed", e);
            }
        }
    }

    private void registerScpCommandSessionListener(ServerSession session) {
        if (null != session) {
            this.scpCommandSessionListener = new ScpCommandSessionListener(Thread.currentThread());
            this.scpCommandSessionListener.registerSession(session);
        }
    }

    /**
     *
     * @return an instance of {@link SshRequestLog} that can be used to log all requests.
     */
    protected abstract SshRequestLog getSshRequestLog();


}
