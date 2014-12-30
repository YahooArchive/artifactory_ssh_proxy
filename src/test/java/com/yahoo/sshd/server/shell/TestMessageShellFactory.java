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
package com.yahoo.sshd.server.shell;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.apache.sshd.common.PtyMode;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestMessageShellFactory {
    @Test
    public void testStringMessage() throws Exception {
        MessageShellFactory factory = new MessageShellFactory("dummy");
        Command command = factory.create();

        command.start(new Environment() {

            @Override
            public void removeSignalListener(SignalListener listener) {}

            @Override
            public Map<PtyMode, Integer> getPtyModes() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> getEnv() {
                return Collections.emptyMap();
            }

            @Override
            public void addSignalListener(SignalListener listener, EnumSet<Signal> signals) {}

            @Override
            public void addSignalListener(SignalListener listener, Signal... signal) {}

            @Override
            public void addSignalListener(SignalListener listener) {}
        });

        Assert.assertTrue(command instanceof InvertedShellWrapper);
        InvertedShellWrapper shellWrapper = (InvertedShellWrapper) command;

        // gotta get the shell out
        Field inField = InvertedShellWrapper.class.getDeclaredField("in");
        inField.setAccessible(true);
        Object object = inField.get(shellWrapper);

        System.out.println(inField);
        System.out.println(object);
    }
}
