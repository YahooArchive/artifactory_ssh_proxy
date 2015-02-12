package com.yahoo.sshd.tools.artifactory;

import org.apache.sshd.common.file.SshFile;

public interface ArtifactoryExceptionInformation {
    /**
     * 
     * @return the sshFile associated with this exception.
     */
    SshFile getFile();
    
    /**
     * =
     * @return the http style status code associated with this exception.
     */
    int getStatusCode();
}
