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
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestFormat {
    @Test
    public void testBadIsZero() {
        Assert.assertEquals(ArtifactMetaDataBuilder.getDateFormatter().formatLongNoException(null), 0);
        Assert.assertEquals(ArtifactMetaDataBuilder.getDateFormatter().formatLongNoException(""), 0);
        Assert.assertEquals(ArtifactMetaDataBuilder.getDateFormatter().formatLongNoException("dasdsa"), 0);
    }
}
