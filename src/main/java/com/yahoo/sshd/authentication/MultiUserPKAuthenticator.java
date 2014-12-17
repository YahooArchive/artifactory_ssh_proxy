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
package com.yahoo.sshd.authentication;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Collection;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

public interface MultiUserPKAuthenticator extends PublickeyAuthenticator {

    void start() throws IOException;

    /**
     * Update the key for a user in the map
     * 
     * @param username
     * @param authorizedKeysStream
     * @throws FileNotFoundException
     */
    // void updateUser(final String username,
    // final InputStream authorizedKeysStream)
    // throws FileNotFoundException;

    /**
     * Authenticate a user in the map
     * 
     * @param username
     * @param publicKey
     * @return
     */
    @Override
    boolean authenticate(String username, PublicKey publicKey, ServerSession session);

    int getNumberOfKeysLoads();

    /**
     * List all of the users this map contains
     */
    Collection<String> getUsers();
}
