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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.jfrog.artifactory.client.model.Item;

public class ChildArtifact {
    private final String uri;
    private final boolean folder;

    public ChildArtifact(String uri, boolean folder) {
        this.uri = uri;
        this.folder = folder;
    }

    public String getUri() {
        return uri;
    }

    public boolean isFolder() {
        return folder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (folder ? 1231 : 1237);
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
        ChildArtifact other = (ChildArtifact) obj;
        if (folder != other.folder) {
            return false;
        }
        if (uri == null) {
            if (other.uri != null) {
                return false;
            }
        } else if (!uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ChildArtifact [uri=" + uri + ", folder=" + folder + "]";
    }

    public static ChildArtifact[] build(List<LinkedHashMap<String, String>> children) {
        if (null == children) {
            return null;
        }

        List<ChildArtifact> list = new LinkedList<>();
        for (LinkedHashMap<String, String> entryList : children) {
            list.add(new ChildArtifact(entryList.get("uri"), Boolean.valueOf(entryList.get("folder")).booleanValue()));
        }

        return list.toArray(new ChildArtifact[] {});
    }

    public static ChildArtifact[] buildFromItemList(List<Item> children) {
        if (null == children) {
            return null;
        }

        List<ChildArtifact> list = new LinkedList<>();
        for (Item item : children) {
            list.add(new ChildArtifact(item.getName(), item.isFolder()));
        }

        return list.toArray(new ChildArtifact[] {});
    }
}
