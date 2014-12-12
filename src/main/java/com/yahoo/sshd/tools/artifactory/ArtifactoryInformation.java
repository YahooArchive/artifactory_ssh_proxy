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
import com.yahoo.sshd.server.settings.SshdProxySettings;

/**
 * This class contains the details about how to authenticate to artifactory.
 * 
 * If you do not use basic http auth, and instead use CAS, you'll need to subclass this class. If your instance requires
 * you hit a CAS url first, and send the token as a header, you'll want to cache that tocken, and add it by overriding
 * {@link ArtifactoryInformation#createNingRequest()} and subclass {@link DefaultNingRequest} to deal with this.
 * 
 * You will also need to subclass {@link SshdProxySettings} and override
 * {@link SshdProxySettings#createArtifactoryInformation(String, String, String)} to return a subclass of your instance
 * of {@link ArtifactoryInformation}
 * 
 * @author areese
 * 
 */
public class ArtifactoryInformation {
    private final String artifactoryUrl;
    private final String artifactoryUsername;
    private final char[] artifactoryPassword;

    public ArtifactoryInformation(String artifactoryUrl, String artifactoryUsername, String artifactoryPassword) {
        this.artifactoryUrl = artifactoryUrl;
        this.artifactoryUsername = artifactoryUsername;
        this.artifactoryPassword = (null != artifactoryPassword) ? artifactoryPassword.toCharArray() : null;
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

    /**
     * Override this to return a NingRequest that automatically adds headers or any other informatnion you need to pass.
     * 
     * @return
     */
    public NingRequest createNingRequest() {
        return new DefaultNingRequest(this);
    }

    /**
     * If you need to pass other headers or cookies to artifactory you'll want to sublcass this, and have
     * {@link ArtifactoryInformation#createNingRequest()} return an instance of your subclass
     * 
     * @author areese
     */
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
