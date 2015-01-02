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

import java.util.EnumSet;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Factory} of {@link Command} that will create a new process and bridge the streams.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */

/**
 * Derived from {@link ProcessShellFactory} When someone ssh's in this should display a configurable message.
 * 
 * The message is displayed by the MessageOutputStream, but this factory takes care of reading the file and caching the
 * string.
 * 
 * @author areese
 * 
 */
public class ForwardingShellFactory implements Factory<Command> {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardingShellFactory.class);

    private String[] command;
    private EnumSet<TtyOptions> ttyOptions;

    public ForwardingShellFactory(EnumSet<TtyOptions> ttyOptions) {
        this(new String[] {}, ttyOptions);
    }

    public ForwardingShellFactory(String[] command, EnumSet<TtyOptions> ttyOptions) {
        this.command = command;
        this.ttyOptions = ttyOptions;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] command) {
        this.command = command;
    }

    @Override
    public Command create() {
        return new ForwardingShellWrapper(ttyOptions);
    }
}
