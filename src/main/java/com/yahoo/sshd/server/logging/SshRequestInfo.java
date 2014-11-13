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
package com.yahoo.sshd.server.logging;

import java.net.InetSocketAddress;

import org.apache.sshd.server.session.ServerSession;

public class SshRequestInfo {
    private final String username;
    private final long size;
    private final String path;
    private final String repoName;
    private final long startTimestamp;
    private final String remoteAddr;
    private final String protocol;
    private final int status;
    private final String method;
    private final int exitValue;

    private SshRequestInfo(String username, long size, String path, String repoName, long startTimestamp,
                    String remoteAddr, String protocol, int status, String method, int exitValue) {
        this.username = username;
        this.size = size;
        this.path = path;
        this.repoName = repoName;
        this.startTimestamp = startTimestamp;
        this.remoteAddr = remoteAddr;
        this.protocol = protocol;
        this.status = status;
        this.method = method;
        this.exitValue = exitValue;
    }

    public String getUserName() {
        return username;
    }

    public long getRequstContentSize() {
        return size;
    }

    public String getRequestPath() {
        return path;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }


    public String getRemoteAddr() {
        return this.remoteAddr;
    }


    public String getProtocol() {
        return this.protocol;
    }


    public int getStatus() {
        return this.status;
    }

    public int getExitValue() {
        return this.exitValue;
    }

    public String getMethod() {
        return this.method;
    }


    public String getRepoName() {
        return this.repoName;
    }

    @Override
    public String toString() {
        return "remoteAddr=" + remoteAddr + ", username=" + username + ", ts=" + startTimestamp + ", method=" + method
                        + ", path=" + repoName + ":" + path + ",status=" + status + ", exitValue=" + exitValue
                        + ", size=" + size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (startTimestamp ^ (startTimestamp >>> 32));
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + status;
        result = prime * result + exitValue;
        result = prime * result + ((null == username) ? 0 : username.hashCode());
        result = prime * result + ((null == repoName) ? 0 : repoName.hashCode());
        result = prime * result + ((null == path) ? 0 : path.hashCode());
        result = prime * result + ((null == remoteAddr) ? 0 : remoteAddr.hashCode());
        result = prime * result + ((null == method) ? 0 : method.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SshRequestInfo other = (SshRequestInfo) obj;
        if (startTimestamp != other.startTimestamp) {
            return false;
        }
        if (size != other.size) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (exitValue != other.exitValue) {
            return false;
        }
        if (null == username) {
            if (null != other.username) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        if (null == remoteAddr) {
            if (null != other.remoteAddr) {
                return false;
            }
        } else if (!remoteAddr.equals(other.remoteAddr)) {
            return false;
        }

        if (null == repoName) {
            if (null != other.repoName) {
                return false;
            }
        } else if (!repoName.equals(other.repoName)) {
            return false;
        }

        if (null == path) {
            if (null != other.path) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (null == method) {
            if (null != other.method) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }

        return true;
    }

    public static final class Builder {
        private String username;
        private long size;
        private String path;
        private String repoName;
        private long startTimestamp;
        private String remoteAddr;
        private String protocol = "ssh2";
        private int status;
        private String method;
        private int exitValue;

        public Builder(ServerSession session) {
            InetSocketAddress remote = (InetSocketAddress) session.getIoSession().getRemoteAddress();
            this.username = session.getUsername();
            this.remoteAddr = remote.getAddress().getHostAddress();
        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setSize(long size) {
            this.size = size;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setRepoName(String repoName) {
            this.repoName = repoName;
            return this;
        }

        public Builder setStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }

        public Builder setRemoteAddr(String remoteAddr) {
            this.remoteAddr = remoteAddr;
            return this;
        }

        public Builder setStatus(int status) {
            this.status = status;
            return this;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setExitValue(int exitValue) {
            this.exitValue = exitValue;
            return this;
        }

        public SshRequestInfo build() {
            return new SshRequestInfo(username, size, path, repoName, startTimestamp, remoteAddr, protocol, status,
                            method, exitValue);
        }


    }

}
