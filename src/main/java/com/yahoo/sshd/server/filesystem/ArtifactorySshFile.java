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
import java.io.PipedInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.sshd.common.file.SshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryAuthorizer;
import com.yahoo.sshd.authorization.ArtifactoryPermTargetType;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaData;
import com.yahoo.sshd.tools.artifactory.ArtifactMetaDataParseFailureException;
import com.yahoo.sshd.tools.artifactory.ArtifactNotFoundException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryFileNotFoundException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryNoWritePermissionException;
import com.yahoo.sshd.tools.artifactory.ChildArtifact;
import com.yahoo.sshd.tools.artifactory.JFrogArtifactoryClientHelper;
import com.yahoo.sshd.utils.streams.AsyncPipedOutputStream;
import com.yahoo.sshd.utils.streams.EmptyInputStream;

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
 * A version of SshFile that queries artifactory using artifactory_client to pretend things are on the FS.
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
public class ArtifactorySshFile implements SshFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactorySshFile.class);

    protected final String filePath;
    protected final String userName;
    protected final ArtifactMetaData metaData;
    protected final JFrogArtifactoryClientHelper jfach;
    protected final String repositoryName;
    protected final ArtifactoryAuthorizer artifactoryAuthorizer;
    private Future<?> uploaderFuture;

    private long size;

    private final HashMap<String, Object> inProperties;

    protected ArtifactorySshFile(final JFrogArtifactoryClientHelper jfach, final String filePath,
                    final String userName, final String repositoryName,
                    final ArtifactoryAuthorizer artifactoryAuthorizer, ArtifactMetaData metaData) {
        this.filePath = filePath;
        this.userName = userName;
        this.metaData = metaData;
        this.jfach = jfach;
        this.repositoryName = repositoryName;
        this.artifactoryAuthorizer = artifactoryAuthorizer;
        this.inProperties = new HashMap<>();
    }

    /**
     * Constructor, internal do not use directly.
     * 
     * @throws IOException
     * @throws ArtifactMetaDataParseFailureException
     */
    public ArtifactorySshFile(final JFrogArtifactoryClientHelper jfach, final String fileName, final String userName,
                    final String repositoryName, final ArtifactoryAuthorizer artifactoryAuthorizer)
                    throws ArtifactMetaDataParseFailureException, IOException {
        this.filePath = fileName;
        this.userName = userName;
        this.jfach = jfach;
        this.repositoryName = repositoryName;
        this.artifactoryAuthorizer = artifactoryAuthorizer;
        // now that we have a filename, we need to call out to artifactory to
        // get the metadata.
        ArtifactMetaData tempData = null;
        try {
            LOGGER.debug("Getting " + fileName + " from Artifactory");
            tempData = jfach.getArtifact(fileName);
        } catch (/* NotFoundException | */ParseException | ArtifactNotFoundException e) {
            // an nfe isn't an issue, we just need mostly empty meta data.
            // we don't log the exception here because it's confusing.
            LOGGER.debug("Getting " + fileName + " from Artifactory failed");
        }

        this.metaData = tempData;
        this.inProperties = new HashMap<>();
    }

    /**
     * Get full name.
     */
    @Override
    public String getAbsolutePath() {
        return filePath;
    }

    /**
     * Get short name.
     */
    @Override
    public String getName() {
        // root - the short name will be '/'
        if (filePath.equals("/")) {
            return "/";
        }

        // strip the last '/'
        String shortName = filePath;
        int filelen = filePath.length();
        if (shortName.charAt(filelen - 1) == '/') {
            shortName = shortName.substring(0, filelen - 1);
        }

        // return from the last '/'
        int slashIndex = shortName.lastIndexOf('/');
        if (slashIndex != -1) {
            shortName = shortName.substring(slashIndex + 1);
        }
        return shortName;
    }

    /**
     * Get owner name
     */
    @Override
    public String getOwner() {
        return userName;
    }

    /**
     * Is it a directory?
     */
    @Override
    public boolean isDirectory() {
        // anything that doesn't exist is a directory
        if (null == metaData) {
            return true;
        }

        return metaData.isDirectory();
    }

    /**
     * Is it a file?
     */
    @Override
    public boolean isFile() {
        // anything that doesn't exist isn't a file
        if (null == metaData) {
            return false;
        }

        return metaData.isFile();
    }

    /**
     * Does this file exists?
     */
    @Override
    public boolean doesExist() {
        return null != metaData;
    }

    /**
     * Get file size. when it's upload to new artifact on artifactory , metaData will be null as the artifact doesn't
     * exist on artifactory yet. so, in that case, size can't be set with metaData but should be set with stream.
     */
    @Override
    public long getSize() {
        if (null != metaData) {
            this.size = metaData.getSize();
        }
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Get last modified time.
     */
    @Override
    public long getLastModified() {
        if (null != metaData) {
            return metaData.getLastModified();
        }
        return 0L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLastModified(long time) {
        // we don't let you set this.
        return false;
    }

    /**
     * Check read permission.
     */
    @Override
    public boolean isReadable() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking for READ access permission for user {} for repository {}", userName,
                            this.repositoryName);
        }
        return artifactoryAuthorizer.authorized(this.repositoryName, userName, ArtifactoryPermTargetType.READ);
    }

    /**
     * Check file write permission.
     */
    @Override
    public boolean isWritable() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking for WRITE access permission for user {} for repository {}", userName,
                            this.repositoryName);
        }
        return artifactoryAuthorizer.authorized(this.repositoryName, userName, ArtifactoryPermTargetType.WRITE);
    }

    /**
     * Check file exec permission.
     */
    @Override
    public boolean isExecutable() {
        // Not sure if we ever care about this. we really can't tell.
        return false;
    }

    /**
     * Has delete permission.
     */
    @Override
    public boolean isRemovable() {
        // Not sure if we care about this. We might, because maven might want to
        // delete snapshots, but for artifactory we probably will let it do
        // clean up.
        return false;
    }

    @Override
    public SshFile getParentFile() {
        // mostly from NativeSshFile, we can just walk up the dir without
        // hitting AF.
        int indexOfSlash = getAbsolutePath().lastIndexOf('/');
        String parentFullName;

        if (indexOfSlash <= 0) {
            parentFullName = "/";
        } else {
            // TODO: indexOfSlash could be -1.
            // we need the slash on the end.
            parentFullName = getAbsolutePath().substring(0, indexOfSlash + 1);
        }

        // we check if the parent FileObject is writable.
        try {
            return createSshFile(parentFullName);
        } catch (ArtifactMetaDataParseFailureException | IOException e) {
            LOGGER.error("Unable to contact artifactory", e);
        }

        return null;
    }

    /**
     * Delete file.
     */
    @Override
    public boolean delete() {
        // we don't allow deletes.
        return false;
    }

    /**
     * Create a new file
     */
    @Override
    public boolean create() throws IOException {
        // we'll automatically create it.
        return !doesExist();
    }

    /**
     * Truncate file to length 0.
     */
    @Override
    public void truncate() throws IOException {
        // we don't support truncate
    }

    /**
     * Move file object.
     */
    @Override
    public boolean move(SshFile destination) {
        // not sure if we'll move files.
        // TODO: not sure we'll support move.
        return false;
    }

    /**
     * Create directory.
     */
    @Override
    public boolean mkdir() {
        // AF will create dirs for us.
        return true;
    }

    /**
     * List files. If not a directory or does not exist, null will be returned.
     */
    @Override
    public List<SshFile> listSshFiles() {
        if (null == metaData || null == metaData.getChildren()) {
            return Collections.emptyList();
        }

        List<SshFile> ret = new ArrayList<>(metaData.getChildren().length);
        // if there are children, we need to get the data.
        // we have a a list of uris as the childern, so we can create new
        // files for each one.
        for (ChildArtifact child : metaData.getChildren()) {
            // we need to uri + child.uri
            String childFilename = this.filePath + "/" + child.getUri();

            try {
                ret.add(createSshFile(childFilename));
            } catch (ArtifactMetaDataParseFailureException | IOException e) {
                LOGGER.error("Unable to get child information for: " + childFilename + " uri: " + metaData.getUri()
                                + child.getUri(), e);
            }
        }

        return ret;
    }

    /**
     * Create output stream for writing.
     */
    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        // permission check
        if (!isWritable()) {
            throw new ArtifactoryNoWritePermissionException("User " + userName
                            + " does not have write permission to repository " + repositoryName + " for file : "
                            + getName(), this);
        }

        if (offset > 0) {
            throw new IOException("Unable to start writing at an offset");
        }

        // move to the appropriate offset and create output stream
        // final RandomAccessFile raf = new RandomAccessFile(file, "rw");

        // TODO: the problem here is that we want to have an inputstream we pass
        // to ning to write to.
        // and they want an outputstream they can right to.
        // the other problem is we need to talk to artifactory async.
        // we want to do the PUT, and not check the response until later.
        // we should probably be checking the response as part of the
        // write/close.
        // which means yet another inputstream to wrap this pipe.

        PipedInputStream snk = new PipedInputStream(16384);
        AsyncPipedOutputStream asyncPipedOutputStream = new AsyncPipedOutputStream(snk);

        try {
            // TODO: eventually we need to call get on this future and deal with
            // the response.
            Map<String, Object> properties = new HashMap<>(inProperties);
            properties.put("X-uploaded-by", userName);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding properties: " + properties);;
            }

            this.uploaderFuture = jfach.putArtifact(snk, filePath, properties, asyncPipedOutputStream.getAsyncHandler());
        } catch (ArtifactNotFoundException e) {
            // FIXME: This might leak, probably should close these:
            // snk.close();
            // asyncPipedOutputStream.close();
            throw new IOException(e);
        }

        return asyncPipedOutputStream;
    }

    /**
     * Create input stream for reading.
     * 
     * this should connect to artifactory, and start getting the file back.
     */
    @Override
    public InputStream createInputStream(long offset) throws IOException {
        // NativeSshFile steps:

        // permission check
        if (!isReadable()) {
            throw new IOException("User " + userName + " does not have read permission to repository " + repositoryName
                            + " for file : " + getName());
        }

        // move to the appropriate offset and create input stream
        // raf.seek(offset);

        // // The IBM jre needs to have both the stream and the random access
        // file
        // objects closed to actually close the file
        // return new FileInputStream(raf.getFD()) {

        // we're going to ignore offset if it's set.
        try {
            InputStream is = jfach.getArtifactContents(filePath);
            if (null == is) {
                // instead of throwing a FileNotFound for a 0 byte file, which
                // just prints a weird message to the consumer.
                // lets return an empty inputstream which will result in a 0
                // byte file.
                return new EmptyInputStream();
            }

            if (offset > 0) {
                is.skip(offset);
            }

            return is;
        } catch (ArtifactNotFoundException e) {
            throw new ArtifactoryFileNotFoundException("File not found: " + filePath, e);
        }
    }

    @Override
    public String toString() {
        return "ArtifactorySshFile [filePath=" + filePath + ", userName=" + userName + ", metaData="
                        + ((null == metaData) ? "null" : metaData) + "]";
    }

    @Override
    public void handleClose() {
        // We need to interrupt the uploader if the the SshFile is closed.
        if (null != this.uploaderFuture) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SshFile is closed. Cancel the uploader task if it is still running.");
            }

            this.uploaderFuture.cancel(true);
        }
    }

    protected ArtifactorySshFile createSshFile(final String childFilename)
                    throws ArtifactMetaDataParseFailureException, IOException {
        return new ArtifactorySshFile(jfach, childFilename, this.userName, this.repositoryName,
                        this.artifactoryAuthorizer);
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
        return getAttributes(followLinks).get(attribute);
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followlLinks) throws IOException {
        Map<Attribute, Object> map = new HashMap<Attribute, Object>();

        map.put(Attribute.Size, Long.valueOf(getSize()));
        map.put(Attribute.IsDirectory, Boolean.valueOf(isDirectory()));
        map.put(Attribute.IsRegularFile, Boolean.valueOf(isFile()));
        map.put(Attribute.IsSymbolicLink, Boolean.valueOf(false));
        map.put(Attribute.LastModifiedTime, Long.valueOf(getLastModified()));
        map.put(Attribute.LastAccessTime, Long.valueOf(getLastModified()));
        map.put(Attribute.Owner, userName);
        map.put(Attribute.Group, userName);

        EnumSet<Permission> p = EnumSet.noneOf(Permission.class);

        if (isReadable()) {
            p.add(Permission.UserRead);
            p.add(Permission.GroupRead);
            p.add(Permission.OthersRead);
        }

        if (isWritable()) {
            p.add(Permission.UserWrite);
            p.add(Permission.GroupWrite);
            p.add(Permission.OthersWrite);
        }

        if (isExecutable()) {
            p.add(Permission.UserExecute);
            p.add(Permission.GroupExecute);
            p.add(Permission.OthersExecute);
        }

        map.put(Attribute.Permissions, p);

        return map;
    }

    @Override
    public String readSymbolicLink() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        Map<Attribute, Object> map = new HashMap<Attribute, Object>();
        map.put(attribute, value);
        setAttributes(map);
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        if (!attributes.isEmpty()) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void createSymbolicLink(SshFile destination) throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getRepoName() {
        return this.repositoryName;
    }

    public void setProperties(Map<String, String> properties) {
        this.inProperties.putAll(properties);
    }

    public Map<String, Object> getProperties() {
        return inProperties;
    }
}
