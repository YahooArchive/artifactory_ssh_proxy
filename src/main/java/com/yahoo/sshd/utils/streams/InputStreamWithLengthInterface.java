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
package com.yahoo.sshd.utils.streams;

import java.io.Closeable;
import java.io.IOException;

public interface InputStreamWithLengthInterface extends Closeable {
    public int read() throws IOException;

    public int read(byte[] b) throws IOException;

    public int read(byte[] b, int off, int len) throws IOException;

    public long skip(long n) throws IOException;

    public int available() throws IOException;

    @Override
    public void close() throws IOException;

    public void mark(int readlimit);

    public void reset() throws IOException;

    public boolean markSupported();

    public int getLength();

}
