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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestArtifactoryMetaData {
    @Test
    public void testDirectoryResponse() throws FileNotFoundException, ArtifactMetaDataParseFailureException,
                    IOException, ParseException {
        ArtifactMetaData data = ArtifactMetaData.decode(loadFile("src/test/resources/json/directory.json"));

        directoryResponse(data);
    }

    @Test
    public void testDirectoryResponseFis() throws FileNotFoundException, ArtifactMetaDataParseFailureException,
                    IOException, ParseException {
        try (FileInputStream fis = new FileInputStream("src/test/resources/json/directory.json")) {
            ArtifactMetaData data = ArtifactMetaData.decode(fis);

            directoryResponse(data);
        }
    }

    // this isn't actually a test.
    @Test(enabled = false)
    public void directoryResponse(ArtifactMetaData data) throws ParseException {
        Assert.assertEquals(data.getCreated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-06T21:03:00.973Z")
                        .getTime());
        Assert.assertEquals(data.getLastModified(), ArtifactMetaDataBuilder.DATE_FORMATTER
                        .format("2013-09-06T21:03:00.973Z").getTime());
        Assert.assertEquals(data.getLastUpdated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-06T21:03:00.973Z")
                        .getTime());
        Assert.assertEquals(data.getRepo(), "ssh-proxy-test");
        Assert.assertEquals(data.getSize(), 0);
        Assert.assertEquals(
                        data.getUri(),
                        // TODO make use of property sshd.artifactoryUrl
                        "http://your-test-artifactory-server:4080/artifactory/api/storage/ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT");
        Assert.assertTrue(data.isDirectory());
        Assert.assertFalse(data.isFile());

        Set<ChildArtifact> childrenSet = new HashSet<>();

        childrenSet.add(new ChildArtifact("/maven-metadata.xml", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130906.210256-1.jar", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130906.210256-1.pom", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130907.001010-2.jar", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130907.001010-2.pom", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130907.043957-3.jar", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130907.043957-3.pom", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130910.215640-4.jar", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130910.215640-4.pom", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130911.161404-5.jar", false));
        childrenSet.add(new ChildArtifact("/sshd-0.0.1-20130911.161404-5.pom", false));

        ChildArtifact[] children = data.getChildren();
        Assert.assertEquals(children.length, childrenSet.size());

        for (ChildArtifact child : children) {
            Assert.assertFalse(child.isFolder());
            Assert.assertTrue(child.getUri().startsWith("/"));
            Assert.assertTrue((child.getUri().endsWith(".jar") || child.getUri().endsWith(".pom") || child.getUri()
                            .endsWith(".xml")));

            child.toString();
        }

        Assert.assertTrue(childrenSet.containsAll(Arrays.asList(children)));
    }

    @Test
    public void testEmptyDirectoryResponse() throws FileNotFoundException, ArtifactMetaDataParseFailureException,
                    IOException, ParseException {
        ArtifactMetaData data = ArtifactMetaData.decode(loadFile("src/test/resources/json/empty.json"));

        Assert.assertEquals(data.getCreated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-13T16:35:03.331Z")
                        .getTime());
        Assert.assertEquals(data.getLastModified(), ArtifactMetaDataBuilder.DATE_FORMATTER
                        .format("2013-09-13T16:35:03.331Z").getTime());
        Assert.assertEquals(data.getLastUpdated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-13T16:35:03.331Z")
                        .getTime());
        Assert.assertEquals(data.getRepo(), "ssh-proxy-test");
        Assert.assertEquals(data.getSize(), 0);
        Assert.assertEquals(data.getUri(),
                        "http://your-test-artifactory-server:4080/artifactory/api/storage/ssh-proxy-test/com/yahoo/sshd/empty");
        Assert.assertTrue(data.isDirectory());
        Assert.assertFalse(data.isFile());
    }

    @Test
    public void testParentDirectoryResponse() throws FileNotFoundException, ArtifactMetaDataParseFailureException,
                    IOException, ParseException {
        ArtifactMetaData data = ArtifactMetaData.decode(loadFile("src/test/resources/json/parent_directory.json"));

        Assert.assertEquals(data.getCreated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-06T21:03:00.973Z")
                        .getTime());
        Assert.assertEquals(data.getLastModified(), ArtifactMetaDataBuilder.DATE_FORMATTER
                        .format("2013-09-06T21:03:00.973Z").getTime());
        Assert.assertEquals(data.getLastUpdated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-06T21:03:00.973Z")
                        .getTime());
        Assert.assertEquals(data.getRepo(), "ssh-proxy-test");
        Assert.assertEquals(data.getSize(), 0);
        Assert.assertEquals(data.getUri(),
                        "http://your-test-artifactory-server:4080/artifactory/api/storage/ssh-proxy-test/com/yahoo/sshd");
        Assert.assertTrue(data.isDirectory());
        Assert.assertFalse(data.isFile());
    }

    @Test
    public void testFileResponse() throws FileNotFoundException, ArtifactMetaDataParseFailureException, IOException,
                    ParseException {
        ArtifactMetaData data = ArtifactMetaData.decode(loadFile("src/test/resources/json/file.json"));

        Assert.assertEquals(data.getCreated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-06T21:03:16.526Z")
                        .getTime());
        Assert.assertEquals(data.getLastModified(), ArtifactMetaDataBuilder.DATE_FORMATTER
                        .format("2013-09-11T16:14:15.840Z").getTime());
        Assert.assertEquals(data.getLastUpdated(), ArtifactMetaDataBuilder.DATE_FORMATTER.format("2013-09-11T16:14:15.840Z")
                        .getTime());
        Assert.assertEquals(data.getRepo(), "ssh-proxy-test");
        Assert.assertEquals(data.getSize(), 755);
        Assert.assertEquals(
                        data.getUri(),
                        "http://your-test-artifactory-server:4080/artifactory/api/storage/ssh-proxy-test/com/yahoo/sshd/0.0.1-SNAPSHOT/maven-metadata.xml");
    }

    @Test
    public void testChildren() {
        ChildArtifact childArtifact = new ChildArtifact("/maven-metadata.xml", false);
        ChildArtifact childArtifact2 = new ChildArtifact("/sshd-0.0.1-20130906.210256-1.jar", false);
        ChildArtifact childArtifact3 = new ChildArtifact("/maven-metadata.xml", true);
        ChildArtifact childArtifact4 = new ChildArtifact(null, true);

        Assert.assertEquals(childArtifact, childArtifact);
        Assert.assertNotEquals(childArtifact, childArtifact2);
        Assert.assertNotEquals(childArtifact, childArtifact3);
        Assert.assertNotEquals(childArtifact, childArtifact4);
        Assert.assertNotEquals(childArtifact, "");
        Assert.assertNotEquals(childArtifact, null);
        Assert.assertNotEquals(childArtifact4, childArtifact);
    }

    static String loadFile(String path) throws FileNotFoundException, IOException {
        StringBuilder str = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))) {

            String line;
            while (null != (line = br.readLine())) {
                str.append(line);
            }

            return str.toString();
        }
    }
}
