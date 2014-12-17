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
package com.yahoo.sshd.authorization.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.authorization.ArtifactoryAuthorizerProviderFactory;
import com.yahoo.sshd.authorization.WideOpenGatesAuthrorizer;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;

public class DefaultArtifactoryAuthorizerProviderFactory implements ArtifactoryAuthorizerProviderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedArtifactoryAuthorizer.class);

    @Override
    public ArtifactoryAuthorizer artifactoryAuthorizerProvider(SshdSettingsInterface settings) {
        if (settings.isDevelopementMode()) {
            LOG.warn("Running in developement mode, many checks are disabled");
            return new WideOpenGatesAuthrorizer(settings);
        }

        return new FileBasedArtifactoryAuthorizer(settings);
    }

}
