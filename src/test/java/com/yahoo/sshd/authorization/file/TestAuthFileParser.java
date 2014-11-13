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

import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;

import org.testng.annotations.Test;

public class TestAuthFileParser {

    @Test()
    public void testAuthFileParser() throws Exception {
        ConcurrentHashMap<String, PermTarget> authorizationHashMap = new ConcurrentHashMap<String, PermTarget>();
        String path = "src/test/resources/auth/auth.txt";
        AuthFileParser.parse(path, authorizationHashMap);
        PermTarget actualPermTargetRepoX = authorizationHashMap.get("repoX");
        Assert.assertNotNull(actualPermTargetRepoX);
        Assert.assertTrue(actualPermTargetRepoX.userExists("a"));
        Assert.assertTrue(actualPermTargetRepoX.userExists("b"));
        Assert.assertFalse(actualPermTargetRepoX.userExists("c"));
        Assert.assertTrue(actualPermTargetRepoX.canWrite("a"));
        Assert.assertTrue(actualPermTargetRepoX.canWrite("b"));
        Assert.assertFalse(actualPermTargetRepoX.canWrite("c"));
        Assert.assertTrue(actualPermTargetRepoX.canRead("a"));
        Assert.assertTrue(actualPermTargetRepoX.canRead("b"));
        Assert.assertTrue(actualPermTargetRepoX.canRead("c"));

        PermTarget actualPermTargetRepoY = authorizationHashMap.get("repoY");
        Assert.assertNotNull(actualPermTargetRepoY);
        Assert.assertFalse(actualPermTargetRepoY.userExists("a"));
        Assert.assertTrue(actualPermTargetRepoY.canRead("a"));
        Assert.assertTrue(actualPermTargetRepoY.canWrite("a"));

        PermTarget actualPermTargetRepoZ = authorizationHashMap.get("repoZ");
        Assert.assertNotNull(actualPermTargetRepoZ);
        Assert.assertTrue(actualPermTargetRepoZ.canWrite("z"));
        Assert.assertTrue(actualPermTargetRepoZ.canRead("z"));
        Assert.assertFalse(actualPermTargetRepoZ.canWrite("a"));
        Assert.assertFalse(actualPermTargetRepoZ.canRead("a"));

    }

}
