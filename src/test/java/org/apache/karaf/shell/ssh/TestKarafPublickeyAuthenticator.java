/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */
package org.apache.karaf.shell.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.AuthorizedKey;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.PublicKeyComparator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestKarafPublickeyAuthenticator {
    PublicKey dsa1;
    PublicKey dsa2;
    PublicKey rsa1;
    PublicKey rsa2;

    @BeforeClass
    public void setup() throws Exception {
        // -0 is an rsa key
        // -10 is a dsa key
        rsa1 = loadFirstKey("src/test/resources/keys/test_ssh_key-0.pub");
        rsa2 = loadFirstKey("src/test/resources/keys/test_ssh_key-1.pub");
        dsa1 = loadFirstKey("src/test/resources/keys/test_ssh_key-10.pub");
        dsa2 = loadFirstKey("src/test/resources/keys/test_ssh_key-11.pub");
    }

    private PublicKey loadFirstKey(String filename) throws FileNotFoundException, IOException,
                    NoSuchAlgorithmException, InvalidKeySpecException {
        try (FileInputStream fis = new FileInputStream(new File(filename))) {
            Map<PublicKey, AuthorizedKey> parseAuthorizedKeys =
                            KarafPublickeyAuthenticator.parseAuthorizedKeys(filename, fis);
            return parseAuthorizedKeys.keySet().iterator().next();
        }
    }

    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] keys() {
        int i = 1;
        return new Object[][] { //
        {rsa1, rsa1, 0, i++}, // 1
                        {rsa2, rsa2, 0, i++}, // 2
                        {dsa1, dsa1, 0, i++},// 3
                        {dsa2, dsa2, 0, i++},// 4
                        {rsa1, dsa1, 1, i++}, // 5
                        {rsa1, rsa2, -1, i++}, // 6
                        {dsa1, dsa2, -1, i++},// 7
                        {dsa2, dsa1, 1, i++},// 8
                        {dsa1, rsa1, -1, i++}, // 9
                        {rsa2, rsa1, 1, i++}, // 10
                        {dsa2, dsa1, 1, i++},// 11
        };
    }

    @Test(dataProvider = "keys")
    public void testComparator(PublicKey left, PublicKey right, int expected, int which) {

        PublicKeyComparator pkc = new PublicKeyComparator();
        Assert.assertEquals(pkc.compare(left, right), expected, " which : " + which + " failed");
    }

    PublicKey publicKey = new PublicKey() {
        private static final long serialVersionUID = 1L;

        @Override
        public String getFormat() {
            return "pkk";
        }

        @Override
        public byte[] getEncoded() {
            return new byte[] {};
        }

        @Override
        public String getAlgorithm() {
            return "areese";
        }
    };

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testComparatorInvalidRsa() {
        PublicKeyComparator pkc = new PublicKeyComparator();
        pkc.compare(publicKey, rsa1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testComparatorInvalidDsa() {
        PublicKeyComparator pkc = new PublicKeyComparator();
        pkc.compare(publicKey, dsa1);
    }

    @Test
    public void testComparatorRsaDsa() {
        PublicKeyComparator pkc = new PublicKeyComparator();
        Assert.assertEquals(pkc.compare(rsa1, dsa1), 1);
    }

    @Test
    public void testComparatorDsaRsa() {
        PublicKeyComparator pkc = new PublicKeyComparator();
        Assert.assertEquals(pkc.compare(dsa1, rsa1), -1);
    }
}
