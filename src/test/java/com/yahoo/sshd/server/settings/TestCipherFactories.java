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
package com.yahoo.sshd.server.settings;

import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.NamedFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.utils.RunnableComponent;

@Test(groups = "unit")
public class TestCipherFactories {
    @Test
    public void testDefaultFactories() throws SshdConfigurationException {
        // IF THIS TEST IS FAILING. Install the unlimited strength jce policy
        // files.
        // arcfour's aren't working on 7u45

        SshdSettingsInterface settings =
                        new SshdProxySettings(1, "hostkey", Collections.<DelegatingCommandFactory>emptyList(), "url",
                                        "user", "pass", new RunnableComponent[] {}, "src/test/resources/auth/auth.txt");

        List<NamedFactory<Cipher>> ciphers = settings.getCiphers();

        Assert.assertTrue(ciphers.size() >= 4);

    }
}
