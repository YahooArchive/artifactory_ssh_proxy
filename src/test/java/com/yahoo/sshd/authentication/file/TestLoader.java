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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@SuppressWarnings("unchecked")
@Test(groups = "unit")
public class TestLoader {
    static final String TEST_RESOURCES_BASE = "src/test/resources/";
    static final String ALWAYS_EMPTY_PATH = TEST_RESOURCES_BASE + "alwaysempty";

    static final String DUMMIES_PATH = TEST_RESOURCES_BASE + "MultiUserPKAuthenticator/home/";
    static final File DUMMIES_PATH_OBJECT = new File(DUMMIES_PATH);

    static final String[] USERS_EXEPECTED_IN_DUMMIES_HOME = new String[] {"areese", "bob", "sam"};

    static final String SPECIAL_DIRECTORY_PATH = TEST_RESOURCES_BASE + "MultiUserPKAuthenticator/dir_test/home/";
    static final File SPECIAL_DIRECTORY_PATH_OBJECT = new File(SPECIAL_DIRECTORY_PATH);

    static final String NON_USER_NAME = "not_a_user";
    static final Path NON_USER_PATH = new File(DUMMIES_PATH + NON_USER_NAME + "/.ssh/"
                    + AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME + "7").toPath();

    static final String DIR_USER_NAME = "dir_test";
    static final Path DIR_USER_PATH = new File(SPECIAL_DIRECTORY_PATH + DIR_USER_NAME + "/.ssh/"
                    + AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME).toPath();

    static final String EMPTY_PATH_THAT_HAS_NO_FILES_STRING = TEST_RESOURCES_BASE
                    + "MultiUserPKAuthenticator/empty_home/";

    static final Path EMPTY_PATH_THAT_HAS_NO_FILES = new File(EMPTY_PATH_THAT_HAS_NO_FILES_STRING).toPath();

    static final String ACTUAL_USER_NAME = "areese";
    static final Path ACTUAL_USER_PATH_WITH_NO_FILES = new File(EMPTY_PATH_THAT_HAS_NO_FILES_STRING + ACTUAL_USER_NAME
                    + "/.ssh/" + AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME).toPath();

    static final Path ACTUAL_USER_PATH_WITH_FILES = new File(DUMMIES_PATH + ACTUAL_USER_NAME + "/.ssh/"
                    + AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME).toPath();

    static final Path ACTUAL_USER_PATH_WITH_EXTRA_SLASHES = new File(DUMMIES_PATH + ACTUAL_USER_NAME + "foo/bar/.ssh/"
                    + AuthorizedKeysFileScanner.AUTHORIZED_KEYS_NAME).toPath();

    /**
     * Basic sanity test to make sure loading works and after load we don't keep updating users.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @SuppressWarnings("resource")
    @Test
    public void testLoader() throws IOException, InterruptedException {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        CountDownLatch wakeupLatch = new CountDownLatch(1);
        AuthorizedKeysFileScanner akfs =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, DUMMIES_PATH_OBJECT,
                                        Collections.EMPTY_LIST);

        Thread t = new Thread(akfs);
        t.start();

        wakeupLatch.await();

        // check that it's still alive
        Assert.assertTrue(t.isAlive());

        Thread.sleep(TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS));

        // make sure we had interactions
        for (String expectedUser : USERS_EXEPECTED_IN_DUMMIES_HOME) {
            Mockito.verify(authorizedKeysMap).updateUser(Matchers.eq(expectedUser), Matchers.any(InputStream.class));
        }

        // check that it's still alive
        Assert.assertTrue(t.isAlive());

        // cause the thread to exit.
        t.interrupt();

        Mockito.verifyNoMoreInteractions(authorizedKeysMap);

        // give it a chance to die.
        Thread.sleep(100L);
        Assert.assertFalse(t.isAlive());
    }

    /**
     * Basic sanity test to make sure a path that doesn't exist throws upon instantiation.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(expectedExceptions = FileNotFoundException.class)
    public void testDoesntExist() throws IOException, InterruptedException {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        CountDownLatch wakeupLatch = new CountDownLatch(1);
        try (AuthorizedKeysFileScanner akfs =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, new File("doesnt/exist"),
                                        Collections.EMPTY_LIST)) {
            akfs.start();
        }

        wakeupLatch.await();
    }

    /**
     * Basic sanity test that when loaded we load bob, sam and areese. All specified in USERS_EXEPECTED_IN_DUMMIES_HOME,
     * and all have public keys in DUMMIES_PATH/<user>/.ssh/authorized_keys
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testLoaderWithDummies() throws IOException, InterruptedException {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        CountDownLatch wakeupLatch = new CountDownLatch(1);
        try (AuthorizedKeysFileScanner akfs =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, DUMMIES_PATH_OBJECT,
                                        Collections.EMPTY_LIST)) {

            Thread t = new Thread(akfs);
            t.start();

            Thread.sleep(TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
            t.interrupt();

            Mockito.verify(authorizedKeysMap, Mockito.times(USERS_EXEPECTED_IN_DUMMIES_HOME.length)).updateUser(
                            Matchers.anyString(), Matchers.any(InputStream.class));

            // make sure we had interactions
            for (String expectedUser : USERS_EXEPECTED_IN_DUMMIES_HOME) {
                Mockito.verify(authorizedKeysMap).updateUser(Matchers.eq(expectedUser), Matchers.any(InputStream.class));
            }
        }

        wakeupLatch.await();
    }

    /**
     * Helper method for testing a filepath that should change.
     * 
     * @param authorizedKeysMap
     * @param username
     * @param directoryPath
     * @param filePathThatChanged
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleWatchedPathChange(MultiUserAuthorizedKeysMap authorizedKeysMap, String username,
                    File directoryPath, Path filePathThatChanged) throws IOException, InterruptedException {
        CountDownLatch wakeupLatch = new CountDownLatch(0);
        try (AuthorizedKeysFileScanner akfs =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, directoryPath,
                                        Collections.EMPTY_LIST)) {
            if (akfs.getFileEventHandler() instanceof FileBasedPKAuthenticatorEventHandler) {
                FileBasedPKAuthenticatorEventHandler auth =
                                (FileBasedPKAuthenticatorEventHandler) akfs.getFileEventHandler();
                auth.handleWatchedPathChanged(filePathThatChanged);
            }
        }

    }

    /**
     * Sanity test, if we call with a specific user, does updateUser get called? Even if there is no files there?
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = FileNotFoundException.class)
    public void testValidHandleWatchedPathChanged() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        try {
            handleWatchedPathChange(authorizedKeysMap, ACTUAL_USER_NAME, DUMMIES_PATH_OBJECT,
                            ACTUAL_USER_PATH_WITH_NO_FILES);
        } finally {
            Mockito.verify(authorizedKeysMap, Mockito.times(0)).updateUser(Matchers.eq(ACTUAL_USER_NAME),
                            Matchers.any(InputStream.class));
        }
    }

    /**
     * Make sure a file without the username isn't executed.
     * 
     * @throws Exception
     */
    @Test
    public void testWrongFileAuthorizedKeysHandleWatchedPathChanged() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        handleWatchedPathChange(authorizedKeysMap, NON_USER_NAME, DUMMIES_PATH_OBJECT, NON_USER_PATH);

        Mockito.verify(authorizedKeysMap, Mockito.times(0)).updateUser(Matchers.eq(NON_USER_NAME),
                        Matchers.any(InputStream.class));
    }

    /**
     * Authorized keys is /home/dir_test/.ssh/authorized_keys (DIR_USER_PATH) is a directory
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = FileNotFoundException.class)
    public void testAuthorizedKeysIsAFileAuthorizedKeysHandleWatchedPathChanged() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        try {
            handleWatchedPathChange(authorizedKeysMap, DIR_USER_NAME, SPECIAL_DIRECTORY_PATH_OBJECT, DIR_USER_PATH);

        } finally {
            Mockito.verify(authorizedKeysMap, Mockito.times(0)).updateUser(Matchers.eq(DIR_USER_NAME),
                            Matchers.any(InputStream.class));
        }
    }

    /**
     * Make sure a file with a username is executed
     * 
     * @throws Exception
     */
    @Test
    public void testRightFilePathToAuthorizedKeysHandleWatchedPathChanged() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        handleWatchedPathChange(authorizedKeysMap, DIR_USER_NAME, DUMMIES_PATH_OBJECT, ACTUAL_USER_PATH_WITH_FILES);

        Mockito.verify(authorizedKeysMap, Mockito.times(0)).updateUser(Matchers.eq(NON_USER_NAME),
                        Matchers.any(InputStream.class));
    }

    /**
     * Make sure a file with a username but not the right number of slashes isn't executed.
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = FileNotFoundException.class)
    public void testWrongFilePathToAuthorizedKeysHandleWatchedPathChanged() throws Exception {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        try {
            handleWatchedPathChange(authorizedKeysMap, DIR_USER_NAME, DUMMIES_PATH_OBJECT,
                            ACTUAL_USER_PATH_WITH_EXTRA_SLASHES);
        } finally {
            Mockito.verify(authorizedKeysMap, Mockito.times(0)).updateUser(Matchers.eq(NON_USER_NAME),
                            Matchers.any(InputStream.class));
        }
    }

    @Test
    public void testExcluded() throws IOException, InterruptedException {
        MultiUserAuthorizedKeysMap authorizedKeysMap = Mockito.mock(MultiUserAuthorizedKeysMap.class);

        FileTreeWalker walker = Mockito.mock(FileTreeWalker.class);

        CountDownLatch wakeupLatch = new CountDownLatch(1);
        try (AuthorizedKeysFileScanner akfs =
                        new AuthorizedKeysFileScanner(wakeupLatch, authorizedKeysMap, new File("/home"),
                                        Arrays.asList(new Path[] {new File("/home/excluded").toPath()}), walker)) {

            akfs.start();
        }
        wakeupLatch.await();
    }
}
