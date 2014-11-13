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

import org.jfrog.artifactory.client.ning.NingRequest;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class ArtifactoryInformation {
    private final String artifactoryUrl;
    private final String artifactoryUsername;
    private final char[] artifactoryPassword;

    public ArtifactoryInformation(String artifactoryUrl, String artifactoryUsername, String artifactoryPassword) {
        this.artifactoryUrl = artifactoryUrl.trim();
        this.artifactoryUsername = artifactoryUsername.trim();
        this.artifactoryPassword = artifactoryPassword.trim().toCharArray();
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public String getArtifactoryUsername() {
        return artifactoryUsername;
    }

    public char[] getArtifactoryPassword() {
        return artifactoryPassword;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactoryPassword == null) ? 0 : artifactoryPassword.hashCode());
        result = prime * result + ((artifactoryUrl == null) ? 0 : artifactoryUrl.hashCode());
        result = prime * result + ((artifactoryUsername == null) ? 0 : artifactoryUsername.hashCode());
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
        ArtifactoryInformation other = (ArtifactoryInformation) obj;
        if (artifactoryPassword == null) {
            if (other.artifactoryPassword != null) {
                return false;
            }
        } else if (!artifactoryPassword.equals(other.artifactoryPassword)) {
            return false;
        }
        if (artifactoryUrl == null) {
            if (other.artifactoryUrl != null) {
                return false;
            }
        } else if (!artifactoryUrl.equals(other.artifactoryUrl)) {
            return false;
        }
        if (artifactoryUsername == null) {
            if (other.artifactoryUsername != null) {
                return false;
            }
        } else if (!artifactoryUsername.equals(other.artifactoryUsername)) {
            return false;
        }
        return true;
    }

    public NingRequest createNingRequest() {
        return new DefaultNingRequest(this);
    }

    public static class DefaultNingRequest implements NingRequest {
        protected final ArtifactoryInformation afInfo;

        public DefaultNingRequest(final ArtifactoryInformation afInfo) {
            this.afInfo = afInfo;
        }

        @Override
        public BoundRequestBuilder getBoundRequestBuilder(BoundRequestBuilder boundRequestBuilder) {
            return boundRequestBuilder;
        }
    }

}
