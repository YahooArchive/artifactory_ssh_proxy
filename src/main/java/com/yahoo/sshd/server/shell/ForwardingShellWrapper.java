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
package com.yahoo.sshd.server.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.apache.sshd.common.util.ThreadUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.apache.sshd.server.shell.Shell;
import org.apache.sshd.server.shell.TtyFilterInputStream;
import org.apache.sshd.server.shell.TtyFilterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a mirror of {@link org.apache.sshd.server.shell.InvertedShellWrapper}, which doesn't have inverted streams.
 * This is for when you are hooking a shell up to an input/outputstream consuming code block instead of launching a
 * process.
 * 
 * This interface is meant to be used with {@link ForwardingShellFactory} class as an implementation of
 * {@link org.apache.sshd.common.Factory<Command>}.
 * 
 * This class implements the {@link Shell} interface as an implementation of {@link org.apache.sshd.server.Command}.
 * 
 * whole point of this class is to have a shell that stays open and provides a way to forward traffic to artifactory
 * without implementing any commands.
 * 
 * {@link Shell} is a mirror of {@link org.apache.sshd.server.shell.InvertedShell} but without the stream inversion
 * 
 * @author areese
 * 
 */
public class ForwardingShellWrapper implements Command, SessionAware, Shell {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardingShellWrapper.class);

    private final Executor executor;
    private final EnumSet<TtyOptions> ttyOptions;

    private TtyFilterInputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private final boolean shutdownExecutor;
    private boolean alive = true;

    public ForwardingShellWrapper(EnumSet<TtyOptions> ttyOptions) {
        // TODO: fix name, we should have some way to identify where this came from.
        this(ttyOptions, ThreadUtils.newSingleThreadExecutor("shell[" + "]"), true);
    }

    public ForwardingShellWrapper(EnumSet<TtyOptions> ttyOptions, Executor executor) {
        this(ttyOptions, executor, false);
    }

    public ForwardingShellWrapper(EnumSet<TtyOptions> ttyOptions, Executor executor, boolean shutdownExecutor) {
        this.ttyOptions = ttyOptions;
        this.executor = executor;
        this.shutdownExecutor = shutdownExecutor;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = new TtyFilterInputStream(ttyOptions, in);
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = new TtyFilterOutputStream(ttyOptions, out, in);
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = new TtyFilterOutputStream(ttyOptions, err, in);
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void setSession(ServerSession session) {
        // TODO: do we care about session's?
        // if (shell instanceof SessionAware) {
        // ((SessionAware) shell).setSession(session);
        // }
    }

    @Override
    public synchronized void start(Environment env) throws IOException {
        // TODO propagate the Environment itself and support signal sending.
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runShell();
            }
        });
        LOGGER.info("Leaving start");
    }

    @Override
    public synchronized void destroy() {
        if (shutdownExecutor && executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }

        if (null != in) {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.debug("in.close", e);
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (IOException e) {
                LOGGER.debug("out.close", e);
            }
        }

        if (null != err) {
            try {
                err.close();
            } catch (IOException e) {
                LOGGER.debug("err.close", e);
            }
        }
    }

    /**
     * Run a shell looking for a ctrl-d.
     */
    protected void runShell() {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Running forwarding shell: in: {} out: {}  err: {}", in, out, err);
            }

            Reader r = new InputStreamReader(in);

            try {
                int c = 0;

                while (alive && -1 != (c = r.read())) {
                    switch (c) {
                        case 0x04:
                            LOGGER.info("leaving.");
                            this.alive = false;
                            return;

                        default:
                            out.write(c);
                            out.flush();
                            break;
                    }
                }

                out.flush();
                err.flush();
            } catch (Exception e) {
                e.printStackTrace();
                this.alive = false;
            }

            out.flush();
            err.flush();
            return;
        } catch (Exception e) {
            destroy();
        } finally {
            callback.onExit(exitValue());
            this.alive = false;
            destroy();
        }
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public OutputStream getErrorStream() {
        return err;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public int exitValue() {
        // TODO Auto-generated method stub
        return 0;
    }
}
