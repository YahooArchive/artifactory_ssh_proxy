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
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    // split for repo name and permission targets
                    String[] repoPermTargets = line.split("=");
                    if (repoPermTargets.length > 1) {
                        String repoName = repoPermTargets[0];
                        String permUserTargets = repoPermTargets[1];
                        // split for permission targets
                        String[] permTargets = permUserTargets.split(",");
                        PermTarget permTarget = new PermTarget();
                        for (String target : permTargets) {
                            String[] targetTypes = target.split(":");
                            if (targetTypes.length > 1) {
                                String targetType = targetTypes[0];
                                String userNameString = targetTypes[1];
                                String[] userNames = userNameString.split("\\|");
                                for (String userName : userNames) {
                                    // check if we already have a user. if so,
                                    // merge them
                                    AuthUser authUser = permTarget.getUser(userName);
                                    if (authUser == null) {
                                        authUser = new AuthUser(userName);
                                    }
                                    if (userName.equals("*")) {
                                        authUser.setAll(true);
                                    }
                                    if (targetType.equals(ArtifactoryPermTargetType.WRITE.name())) {
                                        authUser.setWrite(true);
                                    } else if (targetType.equals(ArtifactoryPermTargetType.READ.name())) {
                                        authUser.setRead(true);
                                    }
                                    permTarget.addUser(authUser);
                                }
                            }
                        }
                        authorizationHashMap.put(repoName, permTarget);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return authorizationHashMap;
    }
}
