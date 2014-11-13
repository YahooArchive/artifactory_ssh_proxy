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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * This class represents the meta data that artifactory will return about an artifact.
 * 
 * @author areese
 * 
 */

// TODO: add path, mimetyype, downloaduri
// FIXME: likely not needed with the jfrog client
@JsonDeserialize(builder = ArtifactMetaDataBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactMetaData {

    private final ChildArtifact[] children;
    private final long created;
    private final long lastModified;
    private final long lastUpdated;
    private final String repo;
    private final long size;
    private final String uri;

    public ArtifactMetaData(ChildArtifact[] childArtifacts, Date created, Date lastModified, Date lastUpdated,
                    String modifiedBy, String repo, long size, String uri) throws ParseException {
        this.children = childArtifacts;
        this.created = (null == created) ? 0 : created.getTime();
        this.lastModified = (null == lastModified) ? 0 : lastModified.getTime();
        this.lastUpdated = (null == lastUpdated) ? 0 : lastUpdated.getTime();
        this.repo = repo;
        this.size = size;
        this.uri = uri;
    }

    public ChildArtifact[] getChildren() {
        return children;
    }

    public long getCreated() {
        return created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getSize() {
        return size;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + (int) created;
        result = prime * result + (int) lastModified;
        result = prime * result + (int) lastUpdated;
        result = prime * result + ((repo == null) ? 0 : repo.hashCode());
        result = prime * result + (int) size;
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
        ArtifactMetaData other = (ArtifactMetaData) obj;
        if (children == null) {
            if (other.children != null) {
                return false;
            }
        } else if (!children.equals(other.children)) {
            return false;
        }
        if (created != other.created) {
            return false;
        }

        if (lastModified != other.lastModified) {
            return false;
        }
        if (lastUpdated != other.lastUpdated) {
            return false;
        }

        if (repo == null) {
            if (other.repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }
        if (size != other.size) {
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
        return "ArtifactMetaData [" + ", children=" + children + ", created=" + created + ", lastModified="
                        + lastModified + ", lastUpdated=" + lastUpdated + ", repo=" + repo + ", size=" + size
                        + ", uri=" + uri + "]";
    }

    public static ArtifactMetaData decode(String json) throws ArtifactMetaDataParseFailureException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, ArtifactMetaData.class);
        } catch (IOException e) {
            throw new ArtifactMetaDataParseFailureException("parsing " + json + " failed", e);
        }
    }

    public static ArtifactMetaData decode(InputStream json) throws ArtifactMetaDataParseFailureException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, ArtifactMetaData.class);
        } catch (IOException e) {
            throw new ArtifactMetaDataParseFailureException("parsing " + json + " failed", e);
        }
    }

    public boolean isDirectory() {
        // no children is different from empty children.
        return null != children;
    }

    public boolean isFile() {
        // no children is different from empty children.
        return null == children;
    }

    public String getRepo() {
        return repo;
    }
}
