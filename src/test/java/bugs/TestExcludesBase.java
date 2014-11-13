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
package bugs;

import java.io.File;
import java.nio.file.Path;

import junit.framework.Assert;

import org.testng.annotations.Test;


@Test(groups = "unit")
public class TestExcludesBase {
    @Test
    public void testExcludesBase() {
        String baseStr = "/foo/bar/home/";

        Path base = new File(baseStr).toPath();
        Path sshDir = new File(base.toFile(), "bar/.ssh").toPath();
        Path authorized_keys = new File(sshDir.toFile(), "authorized_keys").toPath();

        Path authorized_keys_name = new File("authorized_keys").toPath();

        Assert.assertTrue(sshDir.startsWith(base));
        Assert.assertTrue(authorized_keys.startsWith(base));
        Assert.assertTrue(authorized_keys.endsWith(authorized_keys_name));
    }

    @Test
    public void testExcludesAuthKey() {
        String baseStr = "src/test/resources/MultiUserPKAuthenticator/home/areese/";

        Path base = new File(baseStr).toPath();
        Path sshDir = new File(base.toFile(), "bar/.ssh").toPath();
        Path authorized_keys = new File(sshDir.toFile(), "authorized_keys").toPath();

        Path authorized_keys_name = new File("authorized_keys").toPath();

        Assert.assertTrue(sshDir.startsWith(base));
        Assert.assertTrue(authorized_keys.startsWith(base));
        Assert.assertTrue(authorized_keys.endsWith(authorized_keys_name));
    }
}
