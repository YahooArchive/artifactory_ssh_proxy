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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.Configuration;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.common.forward.DenyingTcpipForwarderFactory;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.server.filters.DenyingForwardingFilter;
import com.yahoo.sshd.server.filters.LocalForwardingFilter;
import com.yahoo.sshd.server.filters.TestForwardingFilters;
import com.yahoo.sshd.server.settings.SshdProxySettings.ShellMode;
import com.yahoo.sshd.utils.RunnableComponent;

@Test(groups = "unit")
public class TestOptions {
    @Test
    public void testDefault() throws SshdConfigurationException {
        // TODO: supposedly this test fails under windows.
        // too lazy to check at this point, prob cause I wrote it a long time
        // ago.
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        // tests set
        // -Dsshd.propertiesFile=src/test/resources/sshd_proxy.properties

        SshdSettingsBuilder builder = new SshdSettingsBuilder(new String[] {});
        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.getPort(), 2222);
        Assert.assertTrue(settings.getHostKeyPaths().contains("src/test/resources/conf/sshd_proxy/ssh_host_dsa_key"));

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

        // We expect the filters to be denied by default.
        TestForwardingFilters.testDenyFilter(settings.getForwardingFilter());

        // And the deny?
        Assert.assertTrue(settings.getForwardingFactory() instanceof DenyingTcpipForwarderFactory);

        // default forwarding is off.
        Assert.assertFalse(settings.isForwardingAllowed());
    }

    @Test
    public void testEnableJetty() throws SshdConfigurationException {
        // TODO: supposedly this test fails under windows.
        // too lazy to check at this point, prob cause I wrote it a long time
        // ago.
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        String[] args = new String[] {"-f", ""};
        SshdSettingsBuilder builder = new SshdSettingsBuilder(args);
        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.getPort(), 2222);
        Assert.assertTrue(settings.getHostKeyPaths().contains("src/test/resources/conf/sshd_proxy/ssh_host_dsa_key"));

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

    @Test
    public void testMultipleHostKeys() throws Exception {
        String[] args = new String[] {"-f", "src/test/resources/conf/sshd_proxy/sshd_proxy_hostkeys.properties"};
        SshdSettingsBuilder builder = new SshdSettingsBuilder(args);
        SshdSettingsInterface settings = builder.build();

        Assert.assertTrue(settings.getHostKeyPaths().contains("src/test/resources/keys/test_ssh_key-0"));
        Assert.assertTrue(settings.getHostKeyPaths().contains("src/test/resources/keys/test_ssh_key-10"));
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

    @DataProvider
    public Object[][] roots() {
        return new Object[][] { //
        //
                        {null, "src/test/resources/",}, //
                        {"", "src/test/resources/",}, //
                        {"    ", "src/test/resources/",}, //
                        {" foo_root ", "foo_root/",},//
        };
    }

    @Test(dataProvider = "roots")
    public void testRoot(String input, String expected) {
        // we can assume ROOT is set in the env.
        // later we can play games and configure surefire to test different
        // roots.
        // TODO: remove check for ROOT from env.
        SshdSettingsBuilder sb = new SshdSettingsBuilder();
        Assert.assertEquals(sb.findRoot(input), expected);
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


    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] shells() {
        return new Object[][] {//
        //
                        {ShellMode.MESSAGE, false, DenyingForwardingFilter.class},//
                        {ShellMode.FORWARDING_ECHO_SHELL, true, LocalForwardingFilter.class},//
                        {ShellMode.GROOVY_SHELL, false, DenyingForwardingFilter.class},//
        };
    }

    @SuppressWarnings("rawtypes")
    @Test(dataProvider = "shells")
    public void testForwarding(ShellMode shellMode, boolean forwardingAllowed, Class c)
                    throws SshdConfigurationException {
        SshdSettingsBuilder builder = new SshdSettingsBuilder(new String[] {});
        builder.setSetShellMode(shellMode);

        SshdSettingsInterface settings = builder.build();

        Assert.assertEquals(settings.isForwardingAllowed(), forwardingAllowed);
        Assert.assertTrue(c.isInstance(settings.getForwardingFilter()));
    }

    @Test
    public void testEmptyEnvMapping() throws SshdConfigurationException {
        SshdSettingsBuilder builder = new SshdSettingsBuilder(new String[] {});

        SshdSettingsInterface settings = builder.build();
        Assert.assertNotNull(settings.getEnvToAfPropertyMapping());
        Assert.assertTrue(settings.getEnvToAfPropertyMapping().isEmpty());
    }


    @Test
    public void testEnvMappings() throws SshdConfigurationException {
        SshdSettingsBuilder builder =
                        new SshdSettingsBuilder(new String[] {"-f",
                                        "src/test/resources/conf/sshd_proxy/test_env_mapping.properties"});

        SshdSettingsInterface settings = builder.build();
        Map<String, String> envToAfPropertyMapping = settings.getEnvToAfPropertyMapping();
        Assert.assertNotNull(envToAfPropertyMapping);
        Assert.assertEquals(envToAfPropertyMapping.size(), 13);
        for (Entry<String, String> e : envToAfPropertyMapping.entrySet()) {
            Assert.assertEquals(e.getValue(), "X-SshProxy-" + e.getKey());
        }

        System.err.println(envToAfPropertyMapping);
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = SshdConfigurationException.class)
    public void testBadCommandLine() throws SshdConfigurationException {
        new SshdSettingsBuilder(new String[] {"-g"});
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = SshdConfigurationException.class)
    public void testBadPropertiesFile() throws SshdConfigurationException {
        new SshdSettingsBuilder(new String[] {"-f", "/null"});
    }

    @Test
    public void testFilePathDir() throws SshdConfigurationException {
        SshdSettingsBuilder sb =
                        new SshdSettingsBuilder(
                                        new String[] {
                                                        "-f",
                                                        "file:"
                                                                        + new File(
                                                                                        "src/test/resources/conf/sshd_proxy/sshd_proxy.properties")
                                                                                        .getAbsolutePath()});
        Assert.assertEquals(sb.getConfDir(), "src/test/resources/conf/sshd_proxy/");
    }

    @Test
    public void foo() {
        Map<String, String> m1 = new HashMap<>();
        Map<String, Object> m2 = new HashMap<>();
        m1.put("a", "b");
        m2.putAll(m1);

        System.err.println(m2);
    }
}
