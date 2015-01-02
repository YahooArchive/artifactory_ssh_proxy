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

import groovy.lang.Binding;
import groovy.lang.Closure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.codehaus.groovy.tools.shell.InteractiveShellRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.shell.groovy.GroovyPrompt;
import com.yahoo.sshd.server.shell.groovy.OutputStreamErrorHandler;

/**
 * This is a mirror of {@link org.apache.sshd.server.shell.InvertedShellWrapper}, which doesn't have inverted streams.
 * This is for when you are hooking a shell up to an input/outputstream consuming code block instead of launching a
 * process.
 * 
 * This interface is meant to be used with {@link GroovyShellFactory} class as an implementation of
 * {@link org.apache.sshd.common.Factory<Command>}.
 * 
 * This class implements the {@link Shell} interface as an implementation of {@link org.apache.sshd.server.Command}.
 * 
 * The whole point of this class is to allow a groovy shell to be implemented, and used for development testing.
 * 
 * {@link Shell} is a mirror of {@link org.apache.sshd.server.shell.InvertedShell} but without the stream inversion
 * 
 * @author areese
 * 
 */
public class GroovyShellWrapper implements Command, SessionAware, Shell {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyShellFactory.class);

    private final Executor executor;
    private final EnumSet<TtyOptions> ttyOptions;

    private TtyFilterInputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private final boolean shutdownExecutor;
    private boolean alive = true;

    public GroovyShellWrapper(EnumSet<TtyOptions> ttyOptions) {
        // TODO: fix name
        this(ttyOptions, ThreadUtils.newSingleThreadExecutor("shell[" + "]"), true);
    }

    public GroovyShellWrapper(EnumSet<TtyOptions> ttyOptions, Executor executor) {
        this(ttyOptions, executor, false);
    }

    public GroovyShellWrapper(EnumSet<TtyOptions> ttyOptions, Executor executor, boolean shutdownExecutor) {
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
                runGroovy();
            }
        });
        LOGGER.info("Leaving start");
    }

    @Override
    public synchronized void destroy() {
        // TODO: keep binding somewhere we can kill the groovy shell on destroy
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

    protected void runGroovy() {
        try {
            LOGGER.info("Running groovy shell");
            IO io = new IO(in, out, err);

            LOGGER.info("in: {} out: {}  err: {}", in, out, err);

            // TODO: fix to allow other bindings.
            Binding binding = new Binding();
            binding.setVariable("out", io.out);

            Groovysh groovysh = new Groovysh(binding, io);
            InteractiveShellRunner isr = new InteractiveShellRunner(groovysh, new GroovyPrompt(this));

            isr.setErrorHandler(new OutputStreamErrorHandler(this, err));

            try {
                // groovysh.run((String) null);
                isr.run();
            } catch (Exception e) {
                try (PrintStream ps = new PrintStream(out)) {
                    e.printStackTrace(ps);
                }
                this.alive = false;
            }
        } finally {
            this.alive = false;
            destroy();
            callback.onExit(exitValue());
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
