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
package com.yahoo.sshd.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.file.FileBasedArtifactoryAuthorizer;
import com.yahoo.sshd.server.settings.SshdSettingsInterface;

/**
 * Added this to make doing dev work a lot less painful. I don't want to setup an auth.txt for doing local development,
 * so add a flag. The problem is, we probably should flag this separately, because later we might create a developer
 * version that hosts more bits. When that is done, we'll have to rethink this.
 * 
 * @author areese
 * 
 */
public class WideOpenGatesAuthrorizer implements ArtifactoryAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedArtifactoryAuthorizer.class);

    public WideOpenGatesAuthrorizer(SshdSettingsInterface settings) {

    }

    @Override
    public boolean authorized(String repositoryName, String userName, ArtifactoryPermTargetType permissionTarget) {
        LOG.info("Development mode, blindly allowing user: '{}' access to repo: '{}' with target '{}", userName,
                        repositoryName, permissionTarget);
        return true;
    }

}
