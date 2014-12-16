package com.yahoo.sshd.server.jetty;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.yahoo.sshd.utils.RunnableComponent;

public class TestJettyRunnable {
    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] disabled() {
        return new Object[][] {//
        //
                        {-1, null},//
                        {1, null},//
                        {-1, ""},//
                        {1, ""},//
        };
    }

    @Test(dataProvider = "disabled")
    public void testDisabled(int port, String webapp) throws Exception {
        RunnableComponent rc = new JettyRunnableComponent(port, webapp);
        rc.run();
        rc.close();
    }

    // FIXME: figure out how to get a dynamic port, I made this one up.
    @Test
    public void test() throws Exception {
        RunnableComponent rc = new JettyRunnableComponent(60540, "target/webapps");
        rc.run();
        rc.close();
    }
}
