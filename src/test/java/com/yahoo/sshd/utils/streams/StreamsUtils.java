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
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.DataProvider;

public class StreamsUtils {
    public static enum ReadType {
        BYTEARRAY, BYTEOFFSET, SINGLEBYTE
    }

    @SuppressWarnings("boxing")
    @DataProvider
    public static final Object[][] types() {
        List<Object[]> ret = new ArrayList<>();
        ReadType[] types = {ReadType.BYTEARRAY, ReadType.BYTEOFFSET, ReadType.SINGLEBYTE};

        for (int size : new int[] {1, 10, 100, 1000}) {
            for (ReadType type : types) {
                ret.add(new Object[] {type, size});
            }
        }

        return ret.toArray(new Object[][] {});
    }

    public static int readByteOffset(int length, byte[] payload, InputStream payloadInputStream, int ofs)
                    throws IOException {
        int len = (int) Math.min(length, payload.length - ofs);
        len = payloadInputStream.read(payload, ofs, len);

        return len;
    }

    public static int readByteArray(int length, byte[] payload, InputStream payloadInputStream, int ofs)
                    throws IOException {
        int len = (int) Math.min(length, payload.length);

        byte[] temp = new byte[len];
        len = payloadInputStream.read(temp);
        if (len <= 0) {
            return len;
        }

        System.arraycopy(temp, 0, payload, ofs, len);

        return len;
    }

    public static int readByte(int length, byte[] payload, InputStream payloadInputStream, int ofs) throws IOException {
        int maxlen = (int) Math.min(length, payload.length);
        int len = 0;
        for (len = 0; len < maxlen; len++) {
            int read = payloadInputStream.read();
            if (read < 0) {
                break;
            }
            payload[len + ofs] = (byte) read;
        }

        return len;
    }

    public static int readHelper(ReadType readType, int readSize, byte[] payload, InputStream inputStream, int ofs)
                    throws IOException {
        switch (readType) {
            case SINGLEBYTE:
                return readByte(readSize, payload, inputStream, ofs);

            case BYTEOFFSET:
                return readByteOffset(readSize, payload, inputStream, ofs);

            case BYTEARRAY:
                return readByteArray(readSize, payload, inputStream, ofs);

            default:
                throw new IllegalArgumentException();
        }
    }

    public static void setupInputAndExpected(byte[] input, byte[] expected, int totalLength) {
        for (int i = 0; i < input.length; i++) {
            byte value = (byte) (i - 10);

            if (value < 0) {
                value = 0;
            }

            input[i] = value;
            if (i < totalLength) {
                expected[i] = value;
            }
        }
    }
}
