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
import java.io.OutputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestEmptyStream {
    @Test
    public void testEmptyInputStream() throws IOException {
        InputStream eis = new EmptyInputStream();
        Assert.assertEquals(eis.read(new byte[] {}), -1);
        Assert.assertEquals(eis.read(new byte[] {}, 0, 0), -1);
        Assert.assertEquals(eis.skip(0), 0);
        Assert.assertEquals(eis.available(), 0);
        eis.close();
        eis.mark(0);
        eis.reset();
        Assert.assertEquals(eis.markSupported(), false);
        Assert.assertEquals(eis.read(), -1);
    }

    @Test
    public void testEmptyOutputStream() throws IOException {
        // all we can test here is did it throw? this thing does nothing.
        OutputStream eis = new EmptyOutputStream();
        eis.close();
        eis.write(0);
        eis.write(new byte[] {});
        eis.write(new byte[] {}, 0, 0);
        eis.flush();
    }
}
