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
package com.yahoo.sshd.authorization.file;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestAuthFileParser {

    @DataProvider
    public Object[][] authFiles() {
        return new Object[][] {//
        // gack, need to break this test down better.
        // also need to have bad files.
        {"src/test/resources/auth/auth.txt"},//
        };//
    }

    @Test(dataProvider = "authFiles")
    public void testAuthFileParser(String filePath) throws Exception {
        ConcurrentHashMap<String, PermTarget> authorizationHashMap = new ConcurrentHashMap<String, PermTarget>();

        AuthFileParser.parse(filePath, authorizationHashMap);

        Assert.assertFalse(authorizationHashMap.isEmpty());

        {
            PermTarget actualPermTargetRepoX = authorizationHashMap.get("repoX");
            Set<String> existsInX = new HashSet<String>(Arrays.asList(new String[] {"a", "b"}));
            Set<String> readInX = new HashSet<String>(Arrays.asList(new String[] {"a", "b", "c"}));
            Set<String> writeInX = new HashSet<String>(Arrays.asList(new String[] {"a", "b"}));

            Set<String> notExistsInX = new HashSet<String>(Arrays.asList(new String[] {"c"}));
            Set<String> notReadInX = new HashSet<String>(Arrays.asList(new String[] {}));
            Set<String> notWriteInX = new HashSet<String>(Arrays.asList(new String[] {"c"}));
            validatePerms(actualPermTargetRepoX, existsInX, readInX, writeInX, notExistsInX, notReadInX, notWriteInX);
        }

        {
            PermTarget actualPermTargetRepoY = authorizationHashMap.get("repoY");
            Set<String> existsInY = new HashSet<String>(Arrays.asList(new String[] {}));
            Set<String> readInY = new HashSet<String>(Arrays.asList(new String[] {"a"}));
            Set<String> writeInY = new HashSet<String>(Arrays.asList(new String[] {"a"}));

            Set<String> notExistsInY = new HashSet<String>(Arrays.asList(new String[] {"a"}));
            Set<String> notReadInY = new HashSet<String>(Arrays.asList(new String[] {}));
            Set<String> notWriteInY = new HashSet<String>(Arrays.asList(new String[] {}));

            validatePerms(actualPermTargetRepoY, existsInY, readInY, writeInY, notExistsInY, notReadInY, notWriteInY);
        }

        {
            PermTarget actualPermTargetRepoZ = authorizationHashMap.get("repoZ");
            Set<String> existsInZ = new HashSet<String>(Arrays.asList(new String[] {"z"}));
            Set<String> readInZ = new HashSet<String>(Arrays.asList(new String[] {"z"}));
            Set<String> writeInZ = new HashSet<String>(Arrays.asList(new String[] {"z"}));

            Set<String> notExistsInZ = new HashSet<String>(Arrays.asList(new String[] {"a"}));
            Set<String> notReadInZ = new HashSet<String>(Arrays.asList(new String[] {"a"}));
            Set<String> notWriteInZ = new HashSet<String>(Arrays.asList(new String[] {"a"}));

            validatePerms(actualPermTargetRepoZ, existsInZ, readInZ, writeInZ, notExistsInZ, notReadInZ, notWriteInZ);
        }
    }

    public void validatePerms(PermTarget permTarget, Set<String> usersThatExist, Set<String> usersThatCanRead,
                    Set<String> usersThatCanWrite, Set<String> notUsersThatExist, Set<String> notUsersThatCanRead,
                    Set<String> notUsersThatCanWrite) {
        Assert.assertNotNull(permTarget);

        for (String userName : usersThatExist) {
            Assert.assertTrue(permTarget.userExists(userName), "user '" + userName + "' did not exist");
        }

        for (String userName : usersThatCanRead) {
            Assert.assertTrue(permTarget.canRead(userName), "user '" + userName + "' cannot read");
        }

        for (String userName : usersThatCanWrite) {
            Assert.assertTrue(permTarget.canWrite(userName), "user '" + userName + "' cannot write");
        }

        for (String userName : notUsersThatExist) {
            Assert.assertFalse(permTarget.userExists(userName), "user '" + userName + "' did exist");
        }

        for (String userName : notUsersThatCanRead) {
            Assert.assertFalse(permTarget.canRead(userName), "user '" + userName + "' can read");
        }

        for (String userName : notUsersThatCanWrite) {
            Assert.assertFalse(permTarget.canWrite(userName), "user '" + userName + "' can write");
        }
    }

}
