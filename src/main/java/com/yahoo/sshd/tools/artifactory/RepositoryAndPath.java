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

public class RepositoryAndPath {
    private final String repository;
    private final String path;
    private static final String DEVNULL = "/dev/null";
    private static final int DEVNULL_INDEX = DEVNULL.length();

    static final RepositoryAndPath EMPTY_REPOSITORY = new RepositoryAndPath("", "");

    public RepositoryAndPath(String repository, String path) {
        this.repository = repository;
        this.path = path;
    }

    public String getRepository() {
        return repository;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((repository == null) ? 0 : repository.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RepositoryAndPath other = (RepositoryAndPath) obj;
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (repository == null) {
            if (other.repository != null) {
                return false;
            }
        } else if (!repository.equals(other.repository)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "/" + repository + " : /" + path;
    }

    public boolean isDevNull() {
        return (DEVNULL.equals(repository));
    }

    public static RepositoryAndPath splitRepositoryAndPath(String physicalName) {
        if (null == physicalName) {
            return null;
        }

        if (physicalName.isEmpty()) {
            return RepositoryAndPath.EMPTY_REPOSITORY;
        }

        // TODO: we used to ignore things that didn't start with /.
        // if (!physicalName.startsWith("/"))
        // return physicalName;

        int startIndex = (physicalName.startsWith("/")) ? 1 : 0;
        int x;

        // /dev/null has a slash, so wrangle it differently.
        if (physicalName.startsWith(DEVNULL)) {
            x = DEVNULL_INDEX - 1;
            startIndex = 0;
        } else {
            x = startIndex;
        }

        final int slashIndex = physicalName.indexOf('/', x);
        if (-1 == slashIndex) {
            return RepositoryAndPath.EMPTY_REPOSITORY;
        }

        final String repo = physicalName.substring(startIndex, slashIndex);
        final String path = physicalName.substring(slashIndex + 1);

        return new RepositoryAndPath(repo, path);
    }
}
