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

import java.io.IOException;
import java.nio.charset.Charset;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.shell.SshProxyMessage;
import com.yahoo.sshd.utils.streams.StreamsUtils.ReadType;

@Test(groups = "unit")
public class TestMessageOutputStream {

    private static final byte[] bytes = SshProxyMessage.MESSAGE_STRING.getBytes(Charset.forName("UTF-8"));

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void testReads(ReadType r, int readSize) throws IOException {
        // test reading with max size
        runReadsTests(r, -1);
    }

    @Test(dataProvider = "types", dataProviderClass = StreamsUtils.class)
    public void testSmallReads(ReadType r, int readSize) throws IOException {
        // test reading 10 at a time.
        runReadsTests(r, 10);
    }

    // it's not a test.
    @Test(enabled = false)
    public void runReadsTests(ReadType r, int readSize) throws IOException {
        final int totalLength = bytes.length;
        byte[] payload = new byte[totalLength];

        try (MessageOutputStream payloadInputStream = new MessageOutputStream(SshProxyMessage.MESSAGE_STRING)) {
            int length = totalLength;
            int ofs = 0;

            switch (r) {
                case SINGLEBYTE:
                    Assert.assertEquals(readByte(0, new byte[] {}, payloadInputStream, 0), 0);
                    break;

                case BYTEOFFSET:
                    Assert.assertEquals(readByteOffset(0, new byte[] {}, payloadInputStream, 0), 0);
                    break;

                case BYTEARRAY:
                    Assert.assertEquals(readByteArray(0, new byte[] {}, payloadInputStream, 0), 0);

            }

            while (length > 0) {
                if (readSize < 0) {
                    readSize = length;
                }

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

            Assert.assertEquals(payload, bytes);
        }
    }

    @Test
    public void testMiscFunctions() throws IOException {
        try (MessageOutputStream lbis = new MessageOutputStream(SshProxyMessage.MESSAGE_STRING)) {
            int index = 0;

            Assert.assertTrue(lbis.markSupported());

            lbis.mark(10);

            Assert.assertEquals(lbis.read(), bytes[index++]);

            Assert.assertEquals(lbis.available(), bytes.length - 1);

            Assert.assertEquals(lbis.read(), bytes[index++]);

            Assert.assertEquals(lbis.skip(1), bytes.length - index - 1);
            index++;

            Assert.assertEquals(lbis.read(), bytes[index++]);
            Assert.assertEquals(lbis.read(), bytes[index++]);

            Assert.assertEquals(lbis.skip(bytes.length - index), 0);
            index = bytes.length;

            Assert.assertEquals(lbis.read(), -1);

            lbis.reset();

            for (int i = 0; i < bytes.length; i++) {
                Assert.assertEquals(lbis.read(), bytes[i]);
            }
            Assert.assertEquals(lbis.read(), -1);

            lbis.reset();

            Assert.assertEquals(lbis.skip(bytes.length), 0);
            Assert.assertEquals(lbis.read(), -1);

            lbis.reset();

            Assert.assertEquals(lbis.skip(bytes.length * 2), 0);
            Assert.assertEquals(lbis.read(), -1);
            Assert.assertEquals(lbis.read(new byte[] {0, 0, 0, 0}), -1);
            Assert.assertEquals(lbis.read(new byte[] {0, 0, 0, 0}, 0, 5), -1);

        }
    }
}
