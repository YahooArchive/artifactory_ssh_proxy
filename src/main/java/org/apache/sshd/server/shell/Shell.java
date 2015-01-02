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
package org.apache.sshd.server.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.server.Environment;

import com.yahoo.sshd.server.shell.ForwardingShellWrapper;
import com.yahoo.sshd.server.shell.GroovyShellWrapper;

/**
 * This is a mirror of {@link org.apache.sshd.server.shell.InvertedShell}, which doesn't have inverted streams. This is
 * for when you are hooking a shell up to an input/outputstream consuming code block instead of launching a process.
 * 
 * This interface is meant to be used with {@link ForwardingShellWrapper} or {@link GroovyShellWrapper} as an
 * implementation of {@link org.apache.sshd.common.Factory<Command>}.
 * {@link org.apache.sshd.common.Factory<Command>}.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author areese
 */
public interface Shell {

    /**
     * Starts the shell and will make the streams available for the ssh server to retrieve and use.
     * 
     * @param env
     * @throws Exception
     */
    void start(Environment env) throws IOException;

    /**
     * Returns the input stream used to feed the shell. This method is called after the shell has been started.
     * 
     * @return
     */
    InputStream getInputStream();

    /**
     * Return an OuputStream representing the output stream of the shell.
     * 
     * @return
     */
    OutputStream getOutputStream();

    /**
     * Return an InputStream representing the error stream of the shell.
     * 
     * @return
     */
    OutputStream getErrorStream();

    /**
     * Check if the underlying shell is still alive
     * 
     * @return
     */
    boolean isAlive();

    /**
     * Retrieve the exit value of the shell. This method must only be called when the shell is not alive anymore.
     * 
     * @return the exit value of the shell
     */
    int exitValue();

    /**
     * Destroy the shell. This method can be called by the SSH server to destroy the shell because the client has
     * disconnected somehow.
     */
    void destroy();
}
