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

import org.apache.sshd.server.Command;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.utils.RunnableComponent;

@Test(groups = "unit")
public class TestChainedFactories {

    static final class NumberedDelegateCommandFactory extends DelegatingCommandFactory {
        final int number;

        public NumberedDelegateCommandFactory(int number) {
            this(number, null);
        }

        public NumberedDelegateCommandFactory(int number, DelegatingCommandFactory delegate) {
            super(delegate);
            this.number = number;
        }

        @Override
        public Command createCommand(String command) {
            return null;
        }

        public int getNumber() {
            return number;
        }
    }

    @Test
    public void testDelegatingCf() throws SshdConfigurationException {
        List<DelegatingCommandFactory> cfList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            cfList.add(new NumberedDelegateCommandFactory(i));
        }

        SshdSettingsInterface sshdSettings =
                        new SshdProxySettings(2222, "hostkey", cfList, "url", "user", "pass",
                                        new RunnableComponent[] {}, "src/test/resources/auth/auth.txt", 8080);

        DelegatingCommandFactory commandFactory = sshdSettings.getCommandFactory();
        Assert.assertNotNull(commandFactory);

        int i = 0;
        while (null != commandFactory) {
            Assert.assertTrue(commandFactory instanceof NumberedDelegateCommandFactory);
            Assert.assertEquals(((NumberedDelegateCommandFactory) commandFactory).getNumber(), i++);
            commandFactory = (DelegatingCommandFactory) commandFactory.getDelegate();
        }

        Assert.assertEquals(i, 10);
    }
}
