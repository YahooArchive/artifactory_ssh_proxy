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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.utils.streams.StreamsUtils.ReadType;

@Test(groups = "unit")
public class TestLengthBasedInputStream {

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void test10BytesOf100(ReadType readType, int readSize) throws IOException {
        final int totalLength = 20;
        byte[] input = new byte[100];
        byte[] expected = new byte[totalLength];
        byte[] payload = new byte[totalLength];

        // setup the arrays
        StreamsUtils.setupInputAndExpected(input, expected, totalLength);

        try (LengthBasedInputStream payloadInputStream =
                        new LengthBasedInputStream(new ByteArrayInputStream(input), totalLength)) {
            int length = totalLength;
            int ofs = 0;
            while (length > 0) {
                int len = 0;

                len = StreamsUtils.readHelper(readType, readSize, payload, payloadInputStream, ofs);

                length -= len;
                ofs += len;
            }

            Assert.assertEquals(length, 0);

            Assert.assertEquals(payload, expected);

            Assert.assertEquals(payloadInputStream.getBytesRead(), payload.length);
        }
    }

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void test100BytesOf10(ReadType readType, int readSize) throws IOException {
        final int totalLength = 10;
        byte[] input = new byte[10];
        byte[] expected = new byte[totalLength];
        byte[] payload = new byte[10];

        // setup the arrays
        StreamsUtils.setupInputAndExpected(input, expected, totalLength);

        try (LengthBasedInputStream payloadInputStream =
                        new LengthBasedInputStream(new ByteArrayInputStream(input), totalLength)) {
            int length = totalLength * 2;
            int ofs = 0;
            int passes = 0;
            while (length > 0) {
                int len = 0;

                len = StreamsUtils.readHelper(readType, readSize, payload, payloadInputStream, ofs);

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

            Assert.assertEquals(payloadInputStream.getBytesRead(), payload.length);
        }
    }

    @Test
    public void testMiscFunctions() throws IOException {
        byte[] array = new byte[] {0, 1, 2, 3, 4};
        try (LengthBasedInputStream lbis =
                        new LengthBasedInputStream(new ByteArrayInputStream(array), array.length - 2)) {

            Assert.assertTrue(lbis.markSupported());

            lbis.mark(10);

            Assert.assertEquals(lbis.read(), 0);

            Assert.assertEquals(lbis.available(), array.length - 3);

            Assert.assertEquals(lbis.read(), 1);

            Assert.assertEquals(lbis.skip(1), 1);

            Assert.assertEquals(lbis.read(), -1);
            Assert.assertEquals(lbis.getBytesRead(), 3);

            lbis.reset();

            Assert.assertEquals(lbis.read(), 0);
            Assert.assertEquals(lbis.read(), 1);
            Assert.assertEquals(lbis.read(), 2);
            Assert.assertEquals(lbis.read(), -1);

            Assert.assertEquals(lbis.getBytesRead(), 3);

            lbis.reset();

            Assert.assertEquals(lbis.skip(10), 3);
            Assert.assertEquals(lbis.read(), -1);

        }
    }
}
