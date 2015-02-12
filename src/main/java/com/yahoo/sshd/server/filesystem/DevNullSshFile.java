/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */
package com.yahoo.sshd.server.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaData;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaDataParseFailureException;
import com.yahoo.sshd.utils.streams.EmptyInputStream;
import com.yahoo.sshd.utils.streams.EmptyOutputStream;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This class wraps native file object.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

/**
 * Derived from Mina SshFile
 * 
 * This class exists to act like /dev/null anything copied here will be tossed. it's for testing that you can copy
 * without uploading
 * 
 * some portions of this class come from org.apache.sshd.server.filesystem.NativeSshFile, so copying the asl license.
 * javadoc was copied from NativeSshfile, and ordering was done based on that class as well to make it easier to tell
 * all functions are implemented and doc'd.
 * 
 * @author areese
 * 
 */
/*
 * Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved.
 */
public class DevNullSshFile extends ArtifactorySshFile {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(DevNullSshFile.class);
    private final ArtifactoryAuthorizer artifactoryAuthorizer;

    private static final ArtifactMetaData createMetaData(final String userName)
                    throws ArtifactMetaDataParseFailureException {
        Date now = new Date();
        try {
            return new ArtifactMetaData(null /* childArtifacts */, now /* created */, now, now, userName, "/dev/null",
                            0, "/dev/null");
        } catch (ParseException e) {
            throw new ArtifactMetaDataParseFailureException("dev null file creation failed", e);
        }

    }

    /**
     * Constructor, internal do not use directly.
     * 
     * @throws IOException
     * @throws ArtifactMetaDataParseFailureException
     */
    public DevNullSshFile(final String filePath, final String userName,
                    final ArtifactoryAuthorizer artifactoryAuthorizer) throws ArtifactMetaDataParseFailureException,
                    IOException {
        super(null, filePath, userName, "/dev/null", artifactoryAuthorizer, createMetaData(userName));
        this.artifactoryAuthorizer = artifactoryAuthorizer;
    }

    /**
     * Create output stream for writing.
     */
    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        if (offset > 0) {
            throw new IOException("Unable to start writing at an offset");
        }

        return new EmptyOutputStream();
    }

    /**
     * Create input stream for reading.
     * 
     * this should connect to artifactory, and start getting the file back.
     */
    @Override
    public InputStream createInputStream(long offset) throws IOException {
        return new EmptyInputStream();
    }

    @Override
    public String toString() {
        return "DevNullSshFile [filePath=" + filePath + ", userName=" + userName + ", metaData=" + metaData + "]";
    }

    @Override
    protected ArtifactorySshFile createSshFile(final String childFilename)
                    throws ArtifactMetaDataParseFailureException, IOException {
        return new DevNullSshFile(childFilename, this.userName, this.artifactoryAuthorizer);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }
}
