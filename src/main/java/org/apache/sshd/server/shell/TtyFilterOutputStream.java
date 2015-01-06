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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;


/**
 * From: {@link org.apache.sshd.server.shell.ProcessShellFactory}, but extracted so we can use them without subclassing.
 * 
 * org.apache.sshd.server.shell.ProcessShellFactory does this: ttyOptions = EnumSet.of(TtyOptions.ONlCr);
 * 
 * However, it doesn't seem to work for me. So in our copy of
 * org.apache.sshd.server.shell.TtyFilterOutputStream.TtyFilterOutputStream(EnumSet<TtyOptions>, OutputStream,
 * TtyFilterInputStream), we have a special hack that if TtyOptions.INlCr and TtyOptions.ICrNl are both set, send cr nl
 * instead. no idea if the windows even works.
 * 
 * So in here, we have a special hack.. gack.
 * 
 * @author areese
 * 
 */
public class TtyFilterOutputStream extends FilterOutputStream {
    private final TtyFilterInputStream echo;
    private final EnumSet<TtyOptions> ttyOptions;

    public TtyFilterOutputStream(EnumSet<TtyOptions> ttyOptions, OutputStream out, TtyFilterInputStream echo) {
        super(out);
        this.echo = echo;
        this.ttyOptions = ttyOptions;
    }

    @Override
    public void write(int c) throws IOException {

        /**
         * special hack, because I can't seem to get ssh output to work write. without the \r\n on os-x and rhel6, the
         * terminal doesn't start on a newline. it just eats it, and starts again on the same line. need to test on a
         * non-OS-X terminal
         */
        if (c == '\n' && ttyOptions.contains(TtyOptions.INlCr) && ttyOptions.contains(TtyOptions.ICrNl)) {
            super.write('\r');
            super.write('\n');
        } else {
            if (c == '\n' && ttyOptions.contains(TtyOptions.INlCr)) {
                c = '\r';
            } else if (c == '\r' && ttyOptions.contains(TtyOptions.ICrNl)) {
                c = '\n';
            }
            super.write(c);
        }

        if (ttyOptions.contains(TtyOptions.Echo) && null != echo) {
            echo.write(c);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < len; i++) {
            write(b[i]);
        }
    }
}
