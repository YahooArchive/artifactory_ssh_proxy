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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.authorization.ArtifactoryPermTargetType;

/**
 * This class parses an auth.txt and builds a permissions map of repo -> {@link PermTarget}.
 * 
 * @author areese
 * 
 */
public class AuthFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(AuthFileParser.class);

    public static ConcurrentHashMap<String, PermTarget> parse(final String path,
                    final ConcurrentHashMap<String, PermTarget> authorizationHashMap) throws FileNotFoundException {

        LOG.info("Parsing auth file {}", path);
        File authFile = new File(path);

        if (!authFile.exists()) {
            throw new FileNotFoundException("Auth file doesn't exists in path: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(authFile))) {
            String line;
            int lineIndex = 0;

            while ((line = reader.readLine()) != null) {
                lineIndex++;
                line = line.trim();

                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }

                // split for repo name and permission targets
                String[] repoPermTargets = line.split("=");
                if (repoPermTargets.length <= 1) {
                    LOG.info("Ignoring unparseable auth line {} at {}", line, Integer.valueOf(lineIndex));
                    continue;
                }

                String repoName = repoPermTargets[0];
                String permUserTargets = repoPermTargets[1];

                if (repoName.isEmpty() || permUserTargets.isEmpty()) {
                    LOG.info("Ignoring unparseable auth line {} at {}, repoName or userTargets is empty", line,
                                    Integer.valueOf(lineIndex));
                    continue;
                }

                // split for permission targets
                String[] permTargets = permUserTargets.split(",");

                PermTarget permTarget = new PermTarget();
                for (String target : permTargets) {
                    String[] targetTypes = target.split(":");

                    if (targetTypes.length <= 1) {
                        // no targets.
                        continue;
                    }

                    String targetType = targetTypes[0];
                    String userNameString = targetTypes[1];

                    if (targetType.isEmpty() || userNameString.isEmpty()) {
                        continue;
                    }

                    String[] userNames = userNameString.split("\\|");
                    for (String userName : userNames) {
                        // check if we already have a user. if so,
                        // merge them
                        AuthUser authUser = permTarget.getUser(userName);
                        if (authUser == null) {
                            authUser = new AuthUser(userName);
                        }

                        try {
                            ArtifactoryPermTargetType type = ArtifactoryPermTargetType.parseType(targetType);

                            switch (type) {
                                case READ:
                                    authUser.setRead(true);
                                    break;

                                case WRITE:
                                    authUser.setWrite(true);
                                    break;

                                default:
                                    // unknown types.
                                    break;
                            }

                            permTarget.addUser(authUser);
                        } catch (IllegalArgumentException iae) {
                            LOG.error("Illegal target type: " + targetType);
                        }
                    }
                }
                authorizationHashMap.put(repoName, permTarget);
            } // end while
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return authorizationHashMap;
    }
}
