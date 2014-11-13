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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.logging.SshRequestLog;

/**
 * This commands provide SCP support on both server and client side. Permissions and preservation of access /
 * modification times on files are not supported.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
/**
 * This was adapted from ScpCommand. It is an implementation of AbstractScpCommand, which contains the parts that were
 * removed from ScpCommand when making AbstractScpCommand
 * 
 * @author areese
 * 
 */
public class NewScpCommand extends AbstractScpCommand {
    protected static final Logger LOGGER = LoggerFactory.getLogger(NewScpCommand.class);
    private final SshRequestLog requestLog;

    public NewScpCommand(String args, SshRequestLog requestLog) {
        super(args);
        this.requestLog = requestLog;
    }

    @Override
    protected SshRequestLog getSshRequestLog() {
        return requestLog;
    }


}
