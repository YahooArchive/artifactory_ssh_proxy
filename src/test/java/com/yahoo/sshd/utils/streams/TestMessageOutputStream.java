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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.shell.SshProxyMessage;
import com.yahoo.sshd.utils.streams.StreamsUtils.ReadType;

@Test(groups = "unit")
public class TestMessageOutputStream {

    private static final byte[] bytes = SshProxyMessage.MESSAGE_STRING.getBytes(Charset.forName("UTF-8"));

    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] reads() {
        Object[][] types = StreamsUtils.types();
        List<Object[]> ret = new ArrayList<>();
        // read max, small and medium
        for (int i : new int[] {-1, 10, 100}) {
            for (Object[] params : types) {
                //
                ret.add(new Object[] {params[0], i});
            }
        }

        return ret.toArray(new Object[][] {});
    }

    @Test(dataProvider = "reads")
    public void runReadsTests(ReadType readType, int readSize) throws IOException {
        final int totalLength = bytes.length;
        byte[] payload = new byte[totalLength];

        try (MessageOutputStream payloadInputStream = new MessageOutputStream(SshProxyMessage.MESSAGE_STRING)) {
            int length = totalLength;
            int ofs = 0;

            Assert.assertEquals(StreamsUtils.readHelper(readType, 0, new byte[] {}, payloadInputStream, ofs), 0);

            while (length > 0) {
                if (readSize < 0) {
                    readSize = length;
                }

                int len = 0;

                len = StreamsUtils.readHelper(readType, readSize, payload, payloadInputStream, ofs);

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
