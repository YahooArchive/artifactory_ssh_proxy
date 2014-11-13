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
package com.yahoo.sshd.server.configuration;

import junit.framework.Assert;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.mockito.Mockito;
import org.testng.annotations.Test;


@Test(groups = "unit")
public class TestDenyingForwardingFilter {
    @Test
    public void testDeny() {
        DenyingForwardingFilter deny = new DenyingForwardingFilter();
        Session session = Mockito.mock(Session.class);
        SshdSocketAddress address = Mockito.mock(SshdSocketAddress.class);

        Assert.assertFalse(deny.canForwardAgent(session));
        Assert.assertFalse(deny.canForwardX11(session));
        Assert.assertFalse(deny.canListen(address, session));
        Assert.assertFalse(deny.canConnect(address, session));
    }
}
