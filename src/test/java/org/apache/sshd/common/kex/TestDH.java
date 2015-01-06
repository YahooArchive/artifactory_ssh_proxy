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
package org.apache.sshd.common.kex;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestDH {
    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] keyPairGenerator() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(512);
        return new Object[][] {{1, kpg}};
    }

    @Test(
                    description = "https://issues.apache.org/jira/browse/SSHD-330; https://bugzilla.wikimedia.org/show_bug.cgi?id=53895#c28",
                    invocationCount = 500, dataProvider = "keyPairGenerator")
    public void testDH(int a, KeyPairGenerator kpg) throws Exception {

        KeyPair kp = kpg.generateKeyPair();

        BigInteger Y = ((DHPublicKey) kp.getPublic()).getY();
        DHParameterSpec params = ((DHPublicKey) kp.getPublic()).getParams();
        BigInteger P = params.getP();
        BigInteger G = params.getG();

        DH dh = new DH();
        dh.setF(Y);
        dh.setP(P);
        dh.setG(G);
        dh.getE();
        byte[] actual = dh.getK();

        if (actual[0] == 0) {
            Assert.fail("Found leading 0");
        }
    }
}
