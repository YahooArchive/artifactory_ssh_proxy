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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yahoo.sshd.tools.artifactory.ArtifactoryClientFactory;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelper;

public class AFClientCache implements ArtifactoryClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AFClientCache.class);
    private static final int CLIENT_CACHE_SIZE = 10;
    protected LoadingCache<String, JFrogArtifactoryClientHelper> cache;
    protected ArtifactoryClientFactory clientFactory;
    protected ArtifactoryInformation afInfo;

    public AFClientCache(final ArtifactoryInformation inAfInfo, final ArtifactoryClientFactory inClientFactory) {
        this.clientFactory = inClientFactory;
        this.afInfo = inAfInfo;
        LOGGER.info("Setting up cache for Artifactory Client with max size " + CLIENT_CACHE_SIZE);
        this.cache =
                        CacheBuilder.newBuilder().maximumSize(CLIENT_CACHE_SIZE)
                                        .build(new CacheLoader<String, JFrogArtifactoryClientHelper>() {
                                            @Override
                                            public JFrogArtifactoryClientHelper load(final String repositoryName) {
                                                // no checked exception
                                                if (LOGGER.isDebugEnabled()) {
                                                    LOGGER.debug("Creating new Artifactory Client for repository "
                                                                    + repositoryName);
                                                }
                                                return clientFactory.createJFrogClientHelper(afInfo, repositoryName);
                                            }
                                        });
    }

    @Override
    public JFrogArtifactoryClientHelper createJFrogClientHelper(ArtifactoryInformation afInfo, String repositoryName) {
        return cache.getUnchecked(repositoryName);
    }
}
