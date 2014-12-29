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
package com.yahoo.sshd.authentication.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator;
import org.apache.karaf.shell.ssh.KarafPublickeyAuthenticator.AuthorizedKey;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiUserAuthorizedKeysMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiUserAuthorizedKeysMap.class);

    /**
     * A concurrently modifiable map of user -> public key -> Authorized Key map.
     */
    protected final ConcurrentHashMap<String, Map<PublicKey, AuthorizedKey>> userToPkToAuthKeyMap =
                    new ConcurrentHashMap<>();

    /**
     * Given a user name and a file, update their authorized keys. Closes authorizedKeysStream on completion.
     * 
     * @param username
     * @param authorizedKeysStream
     * @throws FileNotFoundException
     */
    public void updateUser(final String username, final String filename, final InputStream authorizedKeysStream)
                    throws FileNotFoundException {
        if (null == authorizedKeysStream) {
            return;
        }

        try {
            Map<PublicKey, AuthorizedKey> newKeys =
                            Collections.unmodifiableMap(KarafPublickeyAuthenticator.parseAuthorizedKeys(filename,
                                            authorizedKeysStream));

            // TODO: fix exception FNF is wrong.
            if (newKeys.isEmpty()) {
                throw new FileNotFoundException("No keys found");
            }

            userToPkToAuthKeyMap.put(username, newKeys);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updated keys for " + username + " with " + newKeys.size() + " keys");
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.info("Unable to update keys for " + username, e);
        } catch (IOException e) {
            LOGGER.info("Unable to update keys for " + username + " " + e.getMessage());
        } finally {
            try {
                authorizedKeysStream.close();
            } catch (IOException e) {
                LOGGER.debug("close failed", e);
            }
        }
    }

    public boolean authenticate(String username, PublicKey publicKey, ServerSession session) {
        // first we need to see if they have an entry.
        Map<PublicKey, AuthorizedKey> map = userToPkToAuthKeyMap.get(username);

        if (null == map) {
            LOGGER.error("Failed to authenticate unknown user {} from {}.", username, session.getIoSession()
                            .getRemoteAddress());
            return false;
        }

        AuthorizedKey ak = map.get(publicKey);
        if (null == ak) {
            LOGGER.error("Failed authentication of user {} from {} with unknown public key.", username, session
                            .getIoSession().getRemoteAddress());
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Successful authentication of user {} from {} with public key {}.", new Object[] {username,
                            session.getIoSession().getRemoteAddress(), ak.getAlias()});
        }

        return true;
    }

    public Set<String> getUsers() {
        if (null == userToPkToAuthKeyMap || userToPkToAuthKeyMap.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(userToPkToAuthKeyMap.keySet());
    }

    public void start() throws IOException {

    }
}
