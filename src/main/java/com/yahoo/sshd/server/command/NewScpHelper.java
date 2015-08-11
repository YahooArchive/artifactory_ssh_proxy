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
package com.yahoo.sshd.server.command;

import org.apache.sshd.common.SshException;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.scp.ScpHelper;
import org.apache.sshd.common.util.DirectoryScanner;
import org.apache.sshd.server.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.yahoo.sshd.server.filesystem.ArtifactorySshFile;
import com.yahoo.sshd.server.filesystem.NameLengthTuple;
import com.yahoo.sshd.server.logging.LoggingHelper;
import com.yahoo.sshd.tools.artifactory.ArtifactoryFileNotFoundException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryNoReadPermissionException;
import com.yahoo.sshd.tools.artifactory.ArtifactoryNoWritePermissionException;

/**
 * Derived from Mina ScpHelper
 * 
 * A version of ScpHelper that does logging in addition to checking for parent file.
 * 
 * some portions of this class come from {@link ScpHelper}, so copying the asl license.
 * 
 * @author charlesk
 * 
 */
public class NewScpHelper extends ScpHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewScpHelper.class);
    private final LoggingHelper loggingHelper;
    private final Environment env;
    private Map<String, String> envToAfPropertyMapping;

    public NewScpHelper(@Nonnull final InputStream in, @Nonnull final OutputStream out,
                    @Nonnull final FileSystemView root, final LoggingHelper loggingHelper, @Nonnull Environment env,
                    @Nonnull Map<String, String> envToAfPropertyMapping) {
        super(in, out, root);
        this.loggingHelper = loggingHelper;
        this.env = env;
        this.envToAfPropertyMapping = envToAfPropertyMapping;
    }

    @Override
    public void receive(SshFile path, boolean recursive, boolean shouldBeDir, boolean preserve) throws IOException {
        if (shouldBeDir) {
            if (!path.doesExist()) {
                throw new SshException("Target directory " + path.toString() + " does not exists");
            }
            if (!path.isDirectory()) {
                throw new SshException("Target directory " + path.toString() + " is not a directory");
            }
        }
        ack();
        long[] time = null;
        for (;;) {
            String line;
            boolean isDir = false;
            int c = readAck(true);
            switch (c) {
                case -1:
                    return;
                case 'D':
                    isDir = true;
                    //$FALL-THROUGH$
                case 'C':
                    line = ((char) c) + readLine();
                    log.debug("Received header: " + line);
                    break;
                case 'T':
                    line = ((char) c) + readLine();
                    log.debug("Received header: " + line);
                    time = parseTime(line);
                    ack();
                    continue;
                case 'E':
                    line = ((char) c) + readLine();
                    log.debug("Received header: " + line);
                    ack();
                    return;
                default:
                    // a real ack that has been acted upon already
                    continue;
            }

            if (recursive && isDir) {
                receiveDir(line, path, time, preserve);
                time = null;
            } else {
                receiveFile(line, path, time, preserve);
                time = null;
            }

            loggingHelper.doLogging(path);
        }
    }

    @Override
    public void send(List<String> paths, boolean recursive, boolean preserve) throws IOException {
        readAck(false);
        for (String pattern : paths) {
            int idx = pattern.indexOf('*');
            if (idx >= 0) {
                String basedir = "";
                int lastSep = pattern.substring(0, idx).lastIndexOf('/');
                if (lastSep >= 0) {
                    basedir = pattern.substring(0, lastSep);
                    pattern = pattern.substring(lastSep + 1);
                }
                String[] included = new DirectoryScanner(basedir, pattern).scan();
                for (String path : included) {
                    SshFile file = root.getFile(basedir + "/" + path);
                    if (file.isFile()) {
                        sendFile(file, preserve);
                    } else if (file.isDirectory()) {
                        if (!recursive) {
                            out.write(ScpHelper.WARNING);
                            out.write((path + " not a regular file\n").getBytes());
                        } else {
                            sendDir(file, preserve);
                        }
                    } else {
                        out.write(ScpHelper.WARNING);
                        out.write((path + " unknown file type\n").getBytes());
                    }
                    loggingHelper.doLogging(file);
                }
            } else {
                String basedir = "";
                int lastSep = pattern.lastIndexOf('/');
                if (lastSep >= 0) {
                    basedir = pattern.substring(0, lastSep);
                    pattern = pattern.substring(lastSep + 1);
                }
                SshFile file = root.getFile(basedir + "/" + pattern);
                if (null == file) {
                    throw new IOException("Unable to get path for " + pattern);
                }
                if (!file.doesExist()) {
                    throw new ArtifactoryFileNotFoundException(file + ": no such file or directory", file);
                }
                if (file.isFile()) {
                    // Check permission
                    if (!file.isReadable()) {
                        throw new ArtifactoryNoReadPermissionException("User does not have read permission for file: "
                                        + file, file);
                    }
                    sendFile(file, preserve);
                } else if (file.isDirectory()) {
                    if (!recursive) {
                        throw new IOException(file + " not a regular file");
                    } else {
                        sendDir(file, preserve);
                    }
                } else {
                    throw new IOException(file + ": unknown file type");
                }
                loggingHelper.doLogging(file);
            }
        }
    }

    @Override
    public void receiveFile(String header, SshFile path, long[] time, boolean preserve) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Receiving file {}", path);
        }
        if (!header.startsWith("C")) {
            throw new IOException("Expected a C message but got '" + header + "'");
        }

        NameLengthTuple nameLength = validatePerms(header);
        if (log.isDebugEnabled()) {
            log.debug("nameLength: " + nameLength.toString());
        }

        SshFile file = validateFile(path, nameLength);

        resetArtifactorySshFileSize(file, nameLength.getLength());

        // add things from our env.
        addEnvProperties(file);

        writeFileToDisk(file, nameLength);

        if (preserve) {
            Map<SshFile.Attribute, Object> attrs = new HashMap<SshFile.Attribute, Object>();
            attrs.put(SshFile.Attribute.Permissions, fromOctalPerms(nameLength.getPerms()));
            if (time != null) {
                attrs.put(SshFile.Attribute.LastModifiedTime, Long.valueOf(time[0]));
                attrs.put(SshFile.Attribute.LastAccessTime, Long.valueOf(time[1]));
            }
            file.setAttributes(attrs);
        }

        ack();
        readAck(false);
    }

    protected void writeFileToDisk(SshFile file, NameLengthTuple nameLength) throws IOException {
        OutputStream os = file.createOutputStream(0);
        try {
            ack();

            byte[] buffer = new byte[8192];
            long length = nameLength.getLength();
            while (length > 0) {
                int len = (int) Math.min(length, buffer.length);
                len = in.read(buffer, 0, len);
                if (len <= 0) {
                    throw new IOException("End of stream reached");
                }
                os.write(buffer, 0, len);
                length -= len;
            }
        } finally {
            os.close();
        }
    }

    private long[] parseTime(String line) {
        String[] numbers = line.substring(1).split(" ");
        return new long[] {Long.parseLong(numbers[0]), Long.parseLong(numbers[2])};
    }


    protected void resetArtifactorySshFileSize(SshFile file, long filesize) {
        // fix for ARTFACTORY-134, set the sshfile size appropriately
        if (file instanceof ArtifactorySshFile) {
            if (log.isDebugEnabled()) {
                log.debug("fix the ssh file size, new size: " + filesize);
            }
            if (filesize >= 0 && filesize != file.getSize()) {
                ((ArtifactorySshFile) file).setSize(filesize);
            }
        }
    }

    /**
     * 
     * ScpCommand does some permissions parsing in the middle of one of the methods. This was split out to make it
     * easier to override and hold onto the data.
     * 
     * This will get the name and length out of the request and return it.
     * 
     * TODO: if perms matter, they should go in the tuple.
     * 
     * @param header
     * @return NameLengthTuple containing the name and the length.
     * @throws IOException
     */
    protected NameLengthTuple validatePerms(String header) throws IOException {
        String perms = header.substring(1, 5);
        long length = Long.parseLong(header.substring(6, header.indexOf(' ', 6)));
        String name = header.substring(header.indexOf(' ', 6) + 1);
        return new NameLengthTuple(name, length, perms);
    }

    protected SshFile validateFile(SshFile path, NameLengthTuple nameLength) throws IOException {
        SshFile file;
        if (path.doesExist() && path.isDirectory()) {
            file = root.getFile(path, nameLength.getName());
        } else if (path.doesExist() && path.isFile()) {
            file = path;

        } else if (!path.doesExist()
        // we don't care if the parent exists, because AF will create it for us.
        // && path.getParentFile().doesExist()
                        && path.getParentFile().isDirectory()) {
            file = path;
        } else {
            // Add additional info
            throw new IOException("Can not write to " + path + " [" + "path.isFile()=" + path.isFile() //
                            + " path.isDirectory()=" + path.isDirectory() //
                            + " path.doesExist()=" + path.doesExist() //
                            + " path.getParentFile().doesExist()=" + path.getParentFile().doesExist() //
                            + " path.getParentFile().isDirectory()=" + path.getParentFile().isDirectory()//
                            + "]");
        }
        if (file.doesExist() && file.isDirectory()) {
            throw new IOException("File is a directory: " + file);
        } else if (file.doesExist() && !file.isWritable()) {
            throw new ArtifactoryNoWritePermissionException("User does not have write permission for file: " + file);
        }
        return file;
    }

    public void doLogging(Throwable e, String path) {
        loggingHelper.doLogging(e, path);
    }

    private void addEnvProperties(SshFile file) {
        if (file instanceof ArtifactorySshFile) {
            if (log.isDebugEnabled()) {
            }

            Map<String, String> afProperties = new HashMap<>();
            // for every property in the env we need to translate it.
            for (Entry<String, String> e : env.getEnv().entrySet()) {
                String value = e.getValue();
                if (null == value || value.isEmpty()) {
                    continue;
                }

                String envName = e.getKey();
                String propertyName = envToAfPropertyMapping.get(envName);
                if (null != propertyName && !propertyName.isEmpty()) {
                    afProperties.put(propertyName, value);
                }
            }

            ((ArtifactorySshFile) file).setProperties(afProperties);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendFile(SshFile path, boolean preserve) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Sending file {}", path);
        }

        Map<SshFile.Attribute,Object> attrs =  path.getAttributes(true);
        if (preserve) {
            StringBuffer buf = new StringBuffer();
            buf.append("T");
            buf.append(attrs.get(SshFile.Attribute.LastModifiedTime));
            buf.append(" ");
            buf.append("0");
            buf.append(" ");
            buf.append(attrs.get(SshFile.Attribute.LastAccessTime));
            buf.append(" ");
            buf.append("0");
            buf.append("\n");
            out.write(buf.toString().getBytes());
            out.flush();
            readAck(false);
        }
        /*
         * See https://support.jfrog.com/support/tickets/28343 for using stream size instead of an attribute.
         * Also, available() method depends on the http client which could implement the InputStream differently.
         */
        InputStream is = path.createInputStream(0);
        long size = is.available();
        if (LOGGER.isDebugEnabled()){
            LOGGER.debug("attribute file size: {} stream size: {} using stream size instead.", attrs.get(SshFile.Attribute.Size), size);
        }
        StringBuffer buf = new StringBuffer();
        buf.append("C");
        buf.append(preserve ? toOctalPerms((EnumSet<SshFile.Permission>) attrs.get(SshFile.Attribute.Permissions)) : "0644");
        buf.append(" ");
        buf.append(size); // length
        buf.append(" ");
        buf.append(path.getName());
        buf.append("\n");
        out.write(buf.toString().getBytes());
        out.flush();
        readAck(false);

        try {
            byte[] buffer = new byte[8192];
            for (;;) {
                int len = is.read(buffer, 0, buffer.length);
                if (len == -1) {
                    break;
                }
                out.write(buffer, 0, len);
            }
        } finally {
            is.close();
        }
        ack();
        readAck(false);
    }
}
