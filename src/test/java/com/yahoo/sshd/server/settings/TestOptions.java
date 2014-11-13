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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.command.DelegatingCommandFactory;

@Test(groups = "unit")
public class TestOptions {
    // disabled because this fails on windows.
    @Test(enabled = false)
    public void testDefault() throws SshdConfigurationException {
        SshdSettingsBuilder builder = new SshdSettingsBuilder(new String[] {});
        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.getPort(), 9000);
        Assert.assertEquals(settings.getHostKeyPath(), "src/test/resources/conf/sshd_proxy/ssh_host_dsa_key");

        List<String> list = new ArrayList<>();
        for (DelegatingCommandFactory df : settings.getCfInstances()) {
            list.add(df.getClass().getCanonicalName());
        }

        Assert.assertEquals(list, SshdSettingsBuilder.DEFAULT_COMMAND_FACTORIES);
    }
}
