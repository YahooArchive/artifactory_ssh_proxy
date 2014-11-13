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
package com.yahoo.sshd.server.settings;

import org.apache.sshd.server.keyprovider.PEMHostKeyProviderFactory;
import org.apache.sshd.server.keyprovider.SshdPEMHostKeyProviderFactory;

import com.google.inject.AbstractModule;
import com.yahoo.sshd.authorization.ArtifactoryAuthorizerProviderFactory;
import com.yahoo.sshd.authorization.file.DefaultArtifactoryAuthorizerProviderFactory;
import com.yahoo.sshd.server.filesystem.ArtifactoryFileSystemFactory;
import com.yahoo.sshd.server.filesystem.InjectableArtifactoryFileSystemFactory;
import com.yahoo.sshd.server.logging.DefaultRequestLogFactory;
import com.yahoo.sshd.server.logging.RequestLogFactory;

public class SshdSettingsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SshdSettingsFactory.class).to(SshdProxySettingsFactory.class);
        bind(InjectableArtifactoryFileSystemFactory.class).to(ArtifactoryFileSystemFactory.class);
        bind(PEMHostKeyProviderFactory.class).to(SshdPEMHostKeyProviderFactory.class);
        bind(ArtifactoryAuthorizerProviderFactory.class).to(DefaultArtifactoryAuthorizerProviderFactory.class);
        bind(RequestLogFactory.class).to(DefaultRequestLogFactory.class);
    }
}
