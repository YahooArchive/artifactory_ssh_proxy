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
package com.yahoo.sshd.tools.artifactory;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestRepositoryAndPath {

    @SuppressWarnings("boxing")
    @DataProvider
    Object[][] repos() {
        return new Object[][] {//
                        {null, null, null, false},//
                        {"", "", "", false}, //
                        {"/", "", "", false},//
                        {"/repo", "", "", false}, //
                        {"/ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT/", "ssh-proxy-test",
                                        "com/yahoo/sshd/0.0.1-SNAPSHOT/", false}, //
                        {"ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT/", "ssh-proxy-test",
                                        "com/yahoo/sshd/0.0.1-SNAPSHOT/", false}, //
                        {"samIAm/com/foo.jar", "samIAm", "com/foo.jar", false}, //
                        {"/dev/null/abc", "/dev/null", "abc", true}, //
        };
    }

    @Test(dataProvider = "repos")
    public void testDevNull(String physicalName, String repository, String path, boolean devnull) {
        RepositoryAndPath repositoryAndPath = RepositoryAndPath.splitRepositoryAndPath(physicalName);

        if (null == physicalName) {
            Assert.assertNull(repositoryAndPath);
        } else {
            Assert.assertEquals(repositoryAndPath.getRepository(), repository);
            Assert.assertEquals(repositoryAndPath.getPath(), path);
            Assert.assertEquals(repositoryAndPath.isDevNull(), devnull);
        }
    }
}
