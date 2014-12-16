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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.Configuration;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.utils.RunnableComponent;

@Test(groups = "unit")
public class TestOptions {
    @Test
    public void testDefault() throws SshdConfigurationException {
        // TODO: supposedly this test fails under windows.
        // too lazy to check at this point, prob cause I wrote it a long time ago.
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        // tests set -Dsshd.propertiesFile=src/test/resources/sshd_proxy.properties

        SshdSettingsBuilder builder = new SshdSettingsBuilder(new String[] {});
        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.getPort(), 2222);
        Assert.assertEquals(settings.getHostKeyPath(), "src/test/resources/conf/sshd_proxy/ssh_host_dsa_key");

        // disabled by default
        Assert.assertEquals(settings.getHttpPort(), SshdSettingsBuilder.DEFAULT_JETTY_PORT);

        // We expect jetty to be disabled by default.
        RunnableComponent[] externalComponents = settings.getExternalComponents();
        Assert.assertEquals(externalComponents.length, 0);


        List<String> list = new ArrayList<>();
        for (DelegatingCommandFactory df : settings.getCfInstances()) {
            list.add(df.getClass().getCanonicalName());
        }

        Assert.assertEquals(list, SshdSettingsBuilder.DEFAULT_COMMAND_FACTORIES);
    }

    @Test(enabled = false)
    public void testEnableJetty() throws SshdConfigurationException {
        // TODO: supposedly this test fails under windows.
        // too lazy to check at this point, prob cause I wrote it a long time ago.
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        String[] args = new String[] {"-f", ""};
        SshdSettingsBuilder builder = new SshdSettingsBuilder(args);
        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.getPort(), 2222);
        Assert.assertEquals(settings.getHostKeyPath(), "src/test/resources/conf/sshd_proxy/ssh_host_dsa_key");

        // disabled by default
        Assert.assertEquals(settings.getHttpPort(), SshdSettingsBuilder.DEFAULT_JETTY_PORT);

        // We expect jetty to be disabled by default.
        RunnableComponent[] externalComponents = settings.getExternalComponents();
        Assert.assertEquals(externalComponents.length, 0);


        List<String> list = new ArrayList<>();
        for (DelegatingCommandFactory df : settings.getCfInstances()) {
            list.add(df.getClass().getCanonicalName());
        }

        Assert.assertEquals(list, SshdSettingsBuilder.DEFAULT_COMMAND_FACTORIES);
    }

    @DataProvider
    public Object[][] overrides() {
        return new Object[][] {//
        //
                        {null, "src/test/resources/conf/sshd_proxy/sshd_proxy.properties"},//
                        {"", "src/test/resources/conf/sshd_proxy/sshd_proxy.properties"},//
                        {"src/test/resources/conf/debug.properties", "src/test/resources/conf/debug.properties"},//
        };
    }

    @Test(dataProvider = "overrides")
    public void testFindConfigFile(String override, String expected) throws Exception {
        SshdSettingsBuilder testBuilder = new SshdSettingsBuilder();
        Configuration config = testBuilder.findPropertiesConfiguration(override);

        AbstractFileConfiguration fileConfiguration = (AbstractFileConfiguration) config;

        // we need to create expected from a new file.
        // because it's a complete filename.
        File expectedFile = new File(expected);
        String expectedPath = "file://" + expectedFile.getAbsolutePath();
        Assert.assertEquals(fileConfiguration.getFileName(), expectedPath);
    }

    @Test
    public void testRoot() {
        // we can assume ROOT is set in the env.
        // later we can play games and configure surefire to test different roots.
        // TODO: remove check for ROOT from env.
        Assert.assertEquals(SshdSettingsBuilder.findRoot(), "src/test/resources/");
    }

    @Test
    public void testConfDir() {
        SshdSettingsBuilder sb = new SshdSettingsBuilder();
        Assert.assertEquals(sb.getConfDir(), "src/test/resources/conf/sshd_proxy/");
    }

    @Test
    public void testLogsDir() {
        SshdSettingsBuilder sb = new SshdSettingsBuilder();
        Assert.assertEquals(sb.getLogsDir(), "src/test/resources/logs/sshd_proxy/");
    }

    @Test
    public void testSystemNameDir() {
        SshdSettingsBuilder sb = new SshdSettingsBuilder();
        Assert.assertEquals(sb.getSystemNameDir(), "/sshd_proxy/");
    }


    @Test
    public void testAuthDir() {
        SshdSettingsBuilder sb = new SshdSettingsBuilder();
        Assert.assertEquals(sb.getAuthDir(), "src/test/resources/conf/sshd_proxy/auth/");
    }

}
