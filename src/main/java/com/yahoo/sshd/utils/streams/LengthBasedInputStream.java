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

import java.io.IOException;
import java.io.InputStream;

/**
 * Allow only length bytes to be read from an inputstream. The caller must retain the wrapped inputstream and close it
 * correctly, as close is a noop for this class.
 * 
 * @author areese
 * 
 */
public class LengthBasedInputStream extends InputStream implements AutoCloseable {

    private final int totalLength;
    private final InputStream internalStream;
    private int readSoFar = 0;
    private int mark = 0;

    public LengthBasedInputStream(InputStream internalStream, int length) {
        this.totalLength = length;
        this.internalStream = internalStream;
    }

    @Override
    public int read() throws IOException {
        // check if we have a byte left.
        if (readSoFar >= totalLength) {
            return -1;
        }

        int ret = internalStream.read();
        if (ret >= 0) {
            readSoFar++;
        }

        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        // check if we have a byte left.
        if (readSoFar >= totalLength) {
            return -1;
        }

        // check if we can read less?
        int toRead = b.length;
        if (readSoFar + b.length >= totalLength) {
            toRead = totalLength - readSoFar;
        }

        int ret = internalStream.read(b, 0, toRead);
        if (ret >= 0) {
            readSoFar += ret;
        }

        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // check if we have a byte left.
        if (readSoFar >= totalLength) {
            return -1;
        }

        // check if we can read less?
        int toRead = len;
        if (readSoFar + len >= totalLength) {
            toRead = totalLength - readSoFar;
        }

        int ret = internalStream.read(b, off, toRead);
        if (ret >= 0) {
            readSoFar += ret;
        }

        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > totalLength - readSoFar) {
            n = totalLength - readSoFar;
        }

        long intSkipped = internalStream.skip(n);
        readSoFar += intSkipped;

        return intSkipped;
    }

    @Override
    public int available() throws IOException {
        int iAvailable = internalStream.available();

        if (iAvailable > totalLength - readSoFar) {
            iAvailable = totalLength - readSoFar;
        }

        return iAvailable;
    }

    /**
     * You MUST retain the wrapped input stream, and close that. This is written to not allow close to be called.
     */
    @Override
    public void close() throws IOException {
        // this explicitly does not allow close to be called.
    }

    @Override
    public synchronized void mark(int readlimit) {
        internalStream.mark(readlimit);
        mark = readSoFar;
    }

    @Override
    public synchronized void reset() throws IOException {
        internalStream.reset();
        readSoFar = mark;
    }

    @Override
    public boolean markSupported() {
        return internalStream.markSupported();
    }

    public int getBytesRead() {
        return readSoFar;
    }
}
