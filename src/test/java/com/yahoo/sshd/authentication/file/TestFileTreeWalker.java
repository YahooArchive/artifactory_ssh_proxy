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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sshd.authentication.file.FileTreeWalker.FileVisitor;

@Test(groups = "unit")
public class TestFileTreeWalker {
    @Test
    public void testExclusions() throws Exception {
        String homeDir = "src/test/resources/MultiUserPKAuthenticator/home/";
        String baseStr = homeDir + "areese/";
        File baseFile = new File(baseStr);
        Path basePath = baseFile.toPath();

        Path userDir = new File("areese").toPath();
        Path sshDir = new File(".ssh").toPath();

        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        Map<WatchKey, Path> watchKeys = new HashMap<>();
        List<Path> excludedPaths = Collections.emptyList();

        FileTreeWalker ftw =
                        new FileTreeWalker(FileSystems.getDefault().newWatchService(), watchKeys,
                                        new File(homeDir).toPath(), excludedPaths, authorizedKeysMap);

        ftw.registerAll(basePath);

        // first the should's.
        watchKeys = ftw.getWatchKeys();
        for (Entry<WatchKey, Path> entry : watchKeys.entrySet()) {
            Path path = entry.getValue();
            File file = path.toFile();

            Assert.assertTrue(file.isDirectory());

            if (!path.endsWith(sshDir) && !path.endsWith(userDir))
                Assert.fail(" extra path found: " + path.toString());
        }
    }

    @Test
    public void testNestedDirs() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        Map<WatchKey, Path> watchKeys = new HashMap<>();
        List<Path> excludedPaths = Collections.emptyList();

        String base = "src/test/resources/nested/home/";

        Set<String> expected = new HashSet<>();

        for (String s : new String[] {base + "areese/.ssh"}) {
            expected.add(new File(s).getAbsolutePath());
        }

        FileTreeWalker ftw =
                        new FileTreeWalker(FileSystems.getDefault().newWatchService(), watchKeys,
                                        new File(base).toPath(), excludedPaths, authorizedKeysMap);

        File _home_areeseFile = new File(base, "areese");
        Path _home_areese = _home_areeseFile.toPath();

        ftw.registerAll(_home_areese);

        watchKeys = ftw.getWatchKeys();

        Assert.assertEquals(watchKeys.size(), expected.size());
        for (Entry<WatchKey, Path> entry : watchKeys.entrySet()) {
            Path path = entry.getValue();
            System.err.println(path);
            File file = path.toFile();

            Assert.assertTrue(file.isDirectory());
            Assert.assertTrue(expected.contains(file.getAbsolutePath()), "expected to find " + file.getAbsolutePath());
        }

    }

    @Test
    public void testFailedVisit() throws IOException {
        String homeDir = "src/test/resources/MultiUserPKAuthenticator/home/";

        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        Map<WatchKey, Path> watchKeys = new HashMap<>();
        List<Path> excludedPaths = Collections.emptyList();

        FileTreeWalker ftw =
                        new FileTreeWalker(FileSystems.getDefault().newWatchService(), watchKeys,
                                        new File(homeDir).toPath(), excludedPaths, authorizedKeysMap);

        FileVisitor fv = ftw.new FileVisitor();

        Path mockedPath = Mockito.mock(Path.class);

        Assert.assertEquals(fv.visitFileFailed(mockedPath, new IOException()), FileVisitResult.CONTINUE);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testPkExcludes() throws IOException {
        String base = "src/test/resources/home/";
        Path basePath = new File(base).toPath().toAbsolutePath();

        String[] homeDirs = new String[] {"src/test/resources/home/areese", "src/test/resources/home/yam"};

        List<Path> homePaths = new ArrayList<>();
        for (String homeDir : homeDirs) {
            homePaths.add(new File(homeDir).toPath());
        }

        String homey = "src/test/resources/home/yodel";
        Path homeyPath = new File(homey).toPath().toAbsolutePath();

        FileTreeWalker ftw = Mockito.mock(FileTreeWalker.class);
        Mockito.when(ftw.getExcludedPaths()).thenReturn(Arrays.asList(new Path[] {homeyPath}));

        Mockito.when(ftw.getHomeDirectoryBasePath()).thenReturn(basePath);
        Mockito.when(ftw.getHomeDirNameCount()).thenReturn(basePath.getNameCount());

        BasicFileAttributes attrs = Mockito.mock(BasicFileAttributes.class);

        FileVisitor fv = ftw.new FileVisitor();

        Assert.assertEquals(fv.preVisitDirectory(new File(base).toPath(), attrs), FileVisitResult.CONTINUE);

        for (String homeDir : homeDirs) {
            Assert.assertEquals(fv.preVisitDirectory(new File(homeDir).toPath(), attrs), FileVisitResult.CONTINUE);

            Assert.assertEquals(fv.preVisitDirectory(new File(homeDir).toPath().toAbsolutePath(), attrs),
                            FileVisitResult.CONTINUE);

        }

        Assert.assertEquals(fv.preVisitDirectory(new File(homey).toPath(), attrs), FileVisitResult.SKIP_SUBTREE);
        Assert.assertEquals(fv.preVisitDirectory(new File(homey).toPath().toAbsolutePath(), attrs),
                        FileVisitResult.SKIP_SUBTREE);
    }
}
