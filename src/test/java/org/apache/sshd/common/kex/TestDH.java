package org.apache.sshd.common.kex;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

public class TestDH {
    @DataProvider
    public Object[][] keyPairGenerator() throws Exception{
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(512);
        return new Object[][] {{ 1, kpg }};
    }

    @Test(description = "https://issues.apache.org/jira/browse/SSHD-330; https://bugzilla.wikimedia.org/show_bug.cgi?id=53895#c28", invocationCount = 500, dataProvider = "keyPairGenerator")
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

        if(actual[0] == 0){
            Assert.fail("Found leading 0");
        }
    }
}
