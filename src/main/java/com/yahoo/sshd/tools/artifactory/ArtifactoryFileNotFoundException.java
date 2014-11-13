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

import java.io.FileNotFoundException;

import org.apache.sshd.common.file.SshFile;

public class ArtifactoryFileNotFoundException extends FileNotFoundException {

    private static final long serialVersionUID = 1L;
    private SshFile file;

    public ArtifactoryFileNotFoundException(String filePath, Throwable cause) {
        super(filePath);
        this.initCause(cause);
    }

    public ArtifactoryFileNotFoundException(String filePath) {
        super(filePath);
    }

    public ArtifactoryFileNotFoundException(String filePath, SshFile file) {
        super(filePath);
        this.file = file;
    }

    public SshFile getFile() {
        return file;
    }

}
