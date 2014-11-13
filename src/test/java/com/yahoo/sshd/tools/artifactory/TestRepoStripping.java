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
public class TestRepoStripping {
    @DataProvider
    Object[][] repos() {
        return new Object[][] {//
                        {null, null},//
                        {"", RepositoryAndPath.EMPTY_REPOSITORY}, //
                        {"/", RepositoryAndPath.EMPTY_REPOSITORY},//
                        {"/repo", RepositoryAndPath.EMPTY_REPOSITORY}, //
                        {
                                        "/ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT/",
                                        new RepositoryAndPath("ssh-proxy-test",
                                                        "com/yahoo/sshd/0.0.1-SNAPSHOT/")}, //
                        {
                                        "ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT/",
                                        new RepositoryAndPath("ssh-proxy-test",
                                                        "com/yahoo/sshd/0.0.1-SNAPSHOT/")}, //
                        {"samIAm/com/foo.jar", new RepositoryAndPath("samIAm", "com/foo.jar")}, //
        };
    }

    @Test(dataProvider = "repos")
    public void testRepoStripping(String in, RepositoryAndPath expected) {
        RepositoryAndPath actual = RepositoryAndPath.splitRepositoryAndPath(in);
        Assert.assertEquals(actual, expected);
    }
}
