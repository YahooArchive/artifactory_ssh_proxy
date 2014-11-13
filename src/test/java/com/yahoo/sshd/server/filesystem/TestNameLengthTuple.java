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
package com.yahoo.sshd.server.filesystem;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestNameLengthTuple {

    @SuppressWarnings("boxing")
    @DataProvider
    public Object[][] data() {
        return new Object[][] { //
        //
                        {null, 0, null}, //
                        {null, 10, null}, //
                        {"", 10, ""}, //
                        {null, 10, ""}, //
                        {"", 10, null}, //
                        {"a", 10, "b"},//
        };
    }

    @DataProvider
    public Object[][] neData() {
        List<Object[]> ret = new ArrayList<>();

        Object[][] srcData = data();

        for (int index1 = 0; index1 < srcData.length; index1++) {

            final Object[] arr1 = srcData[index1];
            final int srcLength = srcData[index1].length;

            for (int index2 = 0; index2 < srcData.length; index2++) {

                if (index1 == index2)
                    continue;

                final Object[] arr2 = srcData[index2];
                Object[] dest = new Object[srcLength * 2];
                System.arraycopy(arr1, 0, dest, 0, arr1.length);
                System.arraycopy(arr2, 0, dest, arr1.length, srcLength);

                ret.add(dest);
            }
        }

        return ret.toArray(new Object[][] {});

    }

    @Test(dataProvider = "data")
    public void testNameLengthTupleEqualsSame(String name, int length, String perms) {
        NameLengthTuple nlt1 = new NameLengthTuple(name, length, perms);
        NameLengthTuple nlt2 = nlt1;

        Assert.assertEquals(nlt1, nlt2);
        Assert.assertEquals(nlt1.hashCode(), nlt2.hashCode());
        Assert.assertEquals(nlt1.toString(), nlt2.toString());
    }

    @Test(dataProvider = "data")
    public void testNameLengthTupleEqualsNull(String name, int length, String perms) {
        NameLengthTuple nlt1 = new NameLengthTuple(name, length, perms);

        Assert.assertFalse(nlt1.equals(null));
    }

    @Test(dataProvider = "data")
    public void testNameLengthTupleEqualsObject(String name, int length, String perms) {
        NameLengthTuple nlt1 = new NameLengthTuple(name, length, perms);

        Assert.assertFalse(nlt1.equals(new Object()));
    }

    @Test(dataProvider = "data")
    public void testNameLengthTupleEquals(String name, int length, String perms) {
        NameLengthTuple nlt1 = new NameLengthTuple(name, length, perms);
        NameLengthTuple nlt2 = new NameLengthTuple(name, length, perms);

        Assert.assertEquals(nlt1, nlt2);
        Assert.assertEquals(nlt1.hashCode(), nlt2.hashCode());
        Assert.assertEquals(nlt1.toString(), nlt2.toString());
    }

    @Test(dataProvider = "data")
    public void testNameLengthTuple(String name, int length, String perms) {
        NameLengthTuple nlt = new NameLengthTuple(name, length, perms);
        Assert.assertEquals(nlt.getName(), name);
        Assert.assertEquals(nlt.getLength(), length);
        Assert.assertEquals(nlt.getPerms(), perms);
    }

    @Test(dataProvider = "neData")
    public void testNotEquals(String name1, int length1, String perms1, String name2, int length2, String perms2) {
        NameLengthTuple nlt1 = new NameLengthTuple(name1, length1, perms1);
        NameLengthTuple nlt2 = new NameLengthTuple(name2, length2, perms2);
        Assert.assertFalse(nlt1.equals(nlt2));
    }
}
