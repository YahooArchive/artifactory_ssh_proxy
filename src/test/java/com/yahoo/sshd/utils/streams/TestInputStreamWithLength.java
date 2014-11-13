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

import static com.yahoo.sshd.utils.streams.StreamsUtils.readByte;
import static com.yahoo.sshd.utils.streams.StreamsUtils.readByteArray;
import static com.yahoo.sshd.utils.streams.StreamsUtils.readByteOffset;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.utils.streams.StreamsUtils.ReadType;

@Test(groups = "unit")
public class TestInputStreamWithLength {

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void test10BytesOf100(ReadType r, int readSize) throws IOException {
        final int totalLength = 20;
        byte[] input = new byte[100];
        byte[] expected = new byte[totalLength];
        for (int i = 0; i < input.length; i++) {
            byte v = (byte) (i - 10);
            if (v < 0)
                v = 0;
            input[i] = v;
            if (i < totalLength)
                expected[i] = v;
        }

        byte[] payload = new byte[totalLength];

        try (InputStreamWithLength payloadInputStream =
                        new InputStreamWithLength(new ByteArrayInputStream(input), totalLength)) {

            Assert.assertEquals(payloadInputStream.getLength(), totalLength);
            int length = totalLength;
            int ofs = 0;
            while (length > 0) {
                int len = 0;
                switch (r) {
                    case SINGLEBYTE:
                        len = readByte(readSize, payload, payloadInputStream, ofs);
                        break;

                    case BYTEOFFSET:
                        len = readByteOffset(readSize, payload, payloadInputStream, ofs);
                        break;

                    case BYTEARRAY:
                        len = readByteArray(readSize, payload, payloadInputStream, ofs);
                        break;
                }

                length -= len;
                ofs += len;
            }

            Assert.assertEquals(length, 0);

            Assert.assertEquals(payload, expected);

            Assert.assertEquals(payloadInputStream.getLength(), payload.length);
        }
    }

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void test100BytesOf10(ReadType r, int readSize) throws IOException {
        final int totalLength = 10;
        byte[] input = new byte[10];
        byte[] expected = new byte[totalLength];
        for (int i = 0; i < input.length; i++) {
            byte v = (byte) (i - 10);
            if (v < 0)
                v = 0;
            input[i] = v;
            if (i < totalLength)
                expected[i] = v;
        }

        byte[] payload = new byte[10];

        try (InputStreamWithLength payloadInputStream =
                        new InputStreamWithLength(new ByteArrayInputStream(input), totalLength)) {
            int length = totalLength * 2;
            int ofs = 0;
            int passes = 0;
            while (length > 0) {
                int len = 0;
                switch (r) {
                    case SINGLEBYTE:
                        len = readByte(readSize, payload, payloadInputStream, ofs);
                        break;

                    case BYTEOFFSET:
                        len = readByteOffset(readSize, payload, payloadInputStream, ofs);
                        break;

                    case BYTEARRAY:
                        len = readByteArray(readSize, payload, payloadInputStream, ofs);
                        break;
                }
                if (len > 0) {
                    length -= len;
                    ofs += len;
                } else {
                    if (passes++ > 2)
                        break;
                }
            }

            Assert.assertEquals(length, 10);

            Assert.assertEquals(payload, expected);

            Assert.assertEquals(payloadInputStream.getLength(), payload.length);
        }
    }

    @Test
    public void testMiscFunctions() throws IOException {
        byte[] array = new byte[] {0, 1, 2, 3, 4};
        try (InputStreamWithLength lbis = new InputStreamWithLength(new ByteArrayInputStream(array), array.length - 2)) {

            Assert.assertTrue(lbis.markSupported());

            lbis.mark(10);

            Assert.assertEquals(lbis.read(), 0);

            Assert.assertEquals(lbis.available(), array.length - 1);

            Assert.assertEquals(lbis.read(), 1);

            Assert.assertEquals(lbis.skip(1), 1);

            Assert.assertEquals(lbis.read(), 3);
            Assert.assertEquals(lbis.read(), 4);
            Assert.assertEquals(lbis.read(), -1);
            Assert.assertEquals(lbis.getLength(), 3);

            lbis.reset();

            Assert.assertEquals(lbis.read(), 0);
            Assert.assertEquals(lbis.read(), 1);
            Assert.assertEquals(lbis.read(), 2);
            Assert.assertEquals(lbis.read(), 3);
            Assert.assertEquals(lbis.read(), 4);
            Assert.assertEquals(lbis.read(), -1);

            Assert.assertEquals(lbis.getLength(), 3);

            lbis.reset();

            Assert.assertEquals(lbis.skip(10), array.length);
            Assert.assertEquals(lbis.read(), -1);

        }
    }
}
