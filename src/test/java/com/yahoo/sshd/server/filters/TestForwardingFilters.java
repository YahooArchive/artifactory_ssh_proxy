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
package com.yahoo.sshd.server.filters;

import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestForwardingFilters {
    @Test
    public void testDeny() {
        testDenyFilter(new DenyingForwardingFilter());
    }

    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] hosts() {
        return new Object[][] {//
        //
                        {"test", 10, "test", 10, true},//
                        {"test", 10, "test", 11, false},//
                        {"test", 10, "testA", 10, false},//
                        {"test", 10, "testA", 11, false},//
        };
    }

    @Test(dataProvider = "hosts")
    public void testLocalForward(String inHost, int inPort, String compareHost, int comparePort, boolean expected) {
        testLocalForwardFilter(new LocalForwardingFilter(inHost, inPort), compareHost, comparePort, expected);
    }

    /**
     * not a test.
     * 
     * @param filter
     */
    @Test(enabled = false)
    public static void testDenyFilter(ForwardingFilter filter) {
        Session session = Mockito.mock(Session.class);
        SshdSocketAddress address = Mockito.mock(SshdSocketAddress.class);

        Assert.assertFalse(filter.canForwardAgent(session));
        Assert.assertFalse(filter.canForwardX11(session));
        Assert.assertFalse(filter.canListen(address, session));
        Assert.assertFalse(filter.canConnect(address, session));
    }

    /**
     * not a test.
     * 
     * @param filter
     */
    @SuppressWarnings("boxing")
    @Test(enabled = false)
    public static void testLocalForwardFilter(ForwardingFilter filter, String hostname, int port, boolean expected) {
        Session session = Mockito.mock(Session.class);
        SshdSocketAddress address = Mockito.mock(SshdSocketAddress.class);
        Mockito.when(address.getHostName()).thenReturn(hostname);
        Mockito.when(address.getPort()).thenReturn(port);

        Assert.assertFalse(filter.canForwardAgent(session));
        Assert.assertFalse(filter.canForwardX11(session));
        Assert.assertFalse(filter.canListen(address, session));
        Assert.assertEquals(filter.canConnect(address, session), expected);
    }
}
