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
package com.yahoo.sshd.authorization.file;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermTarget {

    private static final Logger LOG = LoggerFactory.getLogger(PermTarget.class);

    private final ConcurrentHashMap<String, AuthUser> authUsers = new ConcurrentHashMap<String, AuthUser>();

    public boolean userExists(String userName) {
        return authUsers.containsKey(userName);
    }

    public boolean canRead(String userName) {
        final AuthUser allUser = authUsers.get("*");
        if (allUser != null && allUser.isAll() && allUser.isRead()) {
            // allowing read for all users
            if (LOG.isDebugEnabled()) {
                LOG.debug("Allowed to read for all users.");
            }
            return true;
        }
        final AuthUser user = authUsers.get(userName);
        if (user == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User name {} not found.", userName);
            }
            return false;
        }
        return user.isRead();
    }

    public boolean canWrite(String userName) {
        final AuthUser allUser = authUsers.get("*");
        if (allUser != null && allUser.isAll() && allUser.isWrite()) {
            // allowing write for all users
            if (LOG.isDebugEnabled()) {
                LOG.debug("Allowed to write for all users.");
            }
            return true;
        }
        final AuthUser user = authUsers.get(userName);
        if (user == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User name {} not found.", userName);
            }
            return false;
        }
        return user.isWrite();
    }

    public void addUser(AuthUser authUser) {
        authUsers.put(authUser.getUserName(), authUser);
    }

    public AuthUser getUser(String userName) {
        return authUsers.get(userName);
    }

}
