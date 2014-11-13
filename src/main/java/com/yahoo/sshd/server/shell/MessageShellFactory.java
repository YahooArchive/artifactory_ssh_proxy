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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.streams.EmptyInputStream;
import com.yahoo.sshd.utils.streams.EmptyOutputStream;
import com.yahoo.sshd.utils.streams.MessageOutputStream;

/**
 * A {@link Factory} of {@link Command} that will create a new process and bridge the streams.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */

/**
 * Derived from ProcessShellFactory When someone ssh's in this should display a configurable message.
 * 
 * The message is displayed by the MessageOutputStream, but this factory takes care of reading the file and caching the
 * string.
 * 
 * @author areese
 * 
 */
public class MessageShellFactory extends ProcessShellFactory {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageShellFactory.class);

    private final byte[] messageBytes;

    public MessageShellFactory(String message) {
        super(new String[] {}, EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr));
        this.messageBytes = message.getBytes(Charset.forName("UTF-8"));
    }

    public MessageShellFactory(File messageFile) throws IOException {
        super(new String[] {}, EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr));

        if (!messageFile.exists()) {
            throw new FileNotFoundException("Message file '" + messageFile.getPath() + "' not found");
        }

        if (!messageFile.canRead()) {
            throw new IOException("Message file '" + messageFile.getPath() + "' cannot be read");
        }

        // otherwise read the file.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(messageFile)))) {
            String line;
            StringBuilder messageBuilder = new StringBuilder(8192);
            while (null != (line = br.readLine())) {
                messageBuilder.append(line);
            }

            this.messageBytes = messageBuilder.toString().getBytes(Charset.forName("UTF-8"));
        }
    }

    @Override
    public Command create() {
        return new InvertedShellWrapper(createShell());
    }

    MessageShell createShell() {
        return new MessageShell();
    }

    final class MessageShell extends ProcessShell implements InvertedShell, Runnable {
        private final TtyFilterOutputStream in;
        private final TtyFilterInputStream out;
        private final TtyFilterInputStream err;

        MessageShell() {
            out = new TtyFilterInputStream(new MessageOutputStream(messageBytes));
            err = new TtyFilterInputStream(new EmptyInputStream());
            in = new TtyFilterOutputStream(new EmptyOutputStream(), err);
        }

        @Override
        public void start(Map<String, String> env) throws IOException {

        }

        @Override
        public OutputStream getInputStream() {
            return in;
        }

        @Override
        public InputStream getOutputStream() {
            return out;
        }

        @Override
        public InputStream getErrorStream() {
            return err;
        }

        @Override
        public boolean isAlive() {
            try {
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {

        }

        @Override
        public void run() {
            // loop writing to the output.
        }
    }

}
