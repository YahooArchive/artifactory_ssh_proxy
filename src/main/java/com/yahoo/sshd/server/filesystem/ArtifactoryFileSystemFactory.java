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

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.tools.artifactory.ArtifactoryInformation;

public class ArtifactoryFileSystemFactory extends NativeFileSystemFactory implements
                InjectableArtifactoryFileSystemFactory {
    protected ArtifactoryInformation afInfo;
    protected ArtifactoryAuthorizer artifactoryAuthorizer;

    public ArtifactoryFileSystemFactory() {

    }

    @Override
    public void setAfInfo(final ArtifactoryInformation afInfo) {
        this.afInfo = afInfo;
    }

    @Override
    public void setArtifactoryAuthorizer(ArtifactoryAuthorizer artifactoryAuthorizer) {
        this.artifactoryAuthorizer = artifactoryAuthorizer;

    }

    /**
     * Create the appropriate user file system view.
     */
    @Override
    public FileSystemView createFileSystemView(final Session session) {
        final String userName = session.getUsername();
        return new ArtifactoryFileSystemView(afInfo, userName, isCaseInsensitive(), artifactoryAuthorizer);
    }

}
