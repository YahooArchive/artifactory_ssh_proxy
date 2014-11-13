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

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Simply outputs a static message. This is used for when someone ssh's to a box to make sure that they can verify
 * things will work.
 * 
 * @author areese
 * 
 */
public class MessageOutputStream extends InputStream {
    private final byte[] buffer;
    private int position = 0;

    private int lastMark = 0;

    public MessageOutputStream(String message) {
        this(message.getBytes(Charset.forName("UTF-8")));
    }

    public MessageOutputStream(byte[] messageBytes) {
        this.buffer = messageBytes;
    }

    @Override
    public int read() {
        if (position >= buffer.length) {
            return -1;
        }
        return buffer[position++];
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (0 == len) {
            return 0;
        }

        if (position >= buffer.length) {
            return -1;
        }

        // start out by trying to read as much as they asked.
        int toread = len;

        // See how much we can read
        int maxReadable = buffer.length - position;

        // if they want to read more than we can, we need to limit to the max we
        // can read.
        if (len >= maxReadable) {
            toread = maxReadable;
        }

        System.arraycopy(buffer, position, b, off, toread);
        position += toread;

        return toread;
    }

    @Override
    public long skip(long n) {
        position += n;
        if (position > buffer.length) {
            position = buffer.length;
        }

        return buffer.length - position;
    }

    @Override
    public int available() {
        return buffer.length - position;
    }

    @Override
    public void close() {

    }

    @Override
    public synchronized void mark(int readlimit) {
        lastMark = position;
    }

    @Override
    public synchronized void reset() {
        position = lastMark;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

}
