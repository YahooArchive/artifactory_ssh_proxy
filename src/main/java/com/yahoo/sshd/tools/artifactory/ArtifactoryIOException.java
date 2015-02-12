package com.yahoo.sshd.tools.artifactory;

import java.io.IOException;

import org.apache.sshd.common.file.SshFile;

public abstract class ArtifactoryIOException extends IOException implements ArtifactoryExceptionInformation {

    private static final long serialVersionUID = 1L;

    private final SshFile file;

    public ArtifactoryIOException(String message, SshFile file) {
        super(message);
        this.file = file;
    }

    @Override
    public SshFile getFile() {
        return file;
    }

}
