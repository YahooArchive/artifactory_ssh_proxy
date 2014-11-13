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

/**
 * This is a simple class for keeping track of a name, length and perms tuple from an ssh based file.
 * 
 * The ssh protocol sends these all together, and to easily delegate it's nice to hang onto them this way.
 * 
 * @author areese
 * 
 */
public class NameLengthTuple {
    private final long length;
    private final String name;
    private final String perms;

    /**
     * @param name
     * @param length
     */
    public NameLengthTuple(String name, long length, String perms) {
        this.name = name;
        this.length = length;
        this.perms = perms;
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public String getPerms() {
        return perms;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (length ^ (length >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((perms == null) ? 0 : perms.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NameLengthTuple other = (NameLengthTuple) obj;
        if (length != other.length) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (perms == null) {
            if (other.perms != null) {
                return false;
            }
        } else if (!perms.equals(other.perms)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NameLengthTuple [length=" + length + ", name=" + name + ", perms=" + perms + "]";
    }
}
