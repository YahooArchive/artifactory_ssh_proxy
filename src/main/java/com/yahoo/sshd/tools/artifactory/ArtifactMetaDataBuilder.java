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

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yahoo.sshd.utils.ThreadSafeSimpleDateFormat;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactMetaDataBuilder {
    private static final ThreadSafeSimpleDateFormat DATE_FORMATTER = new ThreadSafeSimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private List<LinkedHashMap<String, String>> children;
    private String created;
    private String lastModified;
    private String lastUpdated;
    private String modifiedBy;
    private String repo;
    private long size;
    private String uri;

    public List<LinkedHashMap<String, String>> getChildren() {
        return children;
    }

    public String getCreated() {
        return created;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public String getRepo() {
        return repo;
    }

    public long getSize() {
        return size;
    }

    public String getUri() {
        return uri;
    }

    public static ThreadSafeSimpleDateFormat getDateFormatter() {
        return DATE_FORMATTER;
    }

    public ArtifactMetaDataBuilder setChildren(List<LinkedHashMap<String, String>> children) {
        this.children = children;
        return this;
    }

    public ArtifactMetaDataBuilder setCreated(String created) {
        this.created = created;
        return this;
    }

    public ArtifactMetaDataBuilder setLastModified(String lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public ArtifactMetaDataBuilder setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public ArtifactMetaDataBuilder setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    public ArtifactMetaDataBuilder setRepo(String repo) {
        this.repo = repo;
        return this;
    }

    public ArtifactMetaDataBuilder setSize(String size) {
        if (null == size) {
            this.size = 0;
        } else {
            this.size = Long.valueOf(size).longValue();
        }
        return this;
    }

    public ArtifactMetaDataBuilder setUri(String uri) {
        this.uri = uri;
        return this;
    }

    ArtifactMetaData build() throws ParseException {
        return new ArtifactMetaData(ChildArtifact.build(children), getDateFormatter().format(created),
                        getDateFormatter().format(lastModified), getDateFormatter().format(lastUpdated), modifiedBy, repo,
                        getSize(), uri);
    }
}
