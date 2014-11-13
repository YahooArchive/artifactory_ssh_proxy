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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.UploadableArtifact;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.ThreadUtils;
import com.yahoo.sshd.utils.streams.AsyncHandler;

public class JFrogArtifactoryClientHelper {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JFrogArtifactoryClientHelper.class);

    // https://jira.corp.yahoo.com/browse/ARTFACTORY-97
    private static final ExecutorService CACHED_THREAD_POOL = new ThreadPoolExecutor(20, 100, 60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadUtils.DefaultThreadFactory("Static Jfrog uploader"));

    protected final Artifactory afClient;
    protected final String repositoryName;
    protected final RepositoryHandle repository;

    // FIXME: need to inject NingRequestImpl
    public JFrogArtifactoryClientHelper(final ArtifactoryInformation afInfo, final String repositoryName) {
        this.repositoryName = repositoryName;

        // TODO: we may need a new method or child class of Artifactory ning
        // client
        // which doesn't require bouncer username/password
        // TODO: we may need a new headless user for ssh proxy to access
        // artifactory
        // use headless bouncer user, leave password empty now just to satisfy
        // method signature
        this.afClient = createAfClient(afInfo);

        this.repository = afClient.repository(this.repositoryName);
    }

    protected Artifactory createAfClient(final ArtifactoryInformation afInfo) {
        return org.jfrog.artifactory.client.ning.ArtifactoryClient.create(afInfo.getArtifactoryUrl(),
                        afInfo.getArtifactoryUsername(), afInfo.getArtifactoryPassword(), afInfo.createNingRequest());
    }

    public ArtifactMetaData getArtifact(String path) throws ParseException, IOException, ArtifactNotFoundException {
        // we need to get an artifact.
        // it could be a file, it could be a folder, but their client doesn't
        // give us the ability to find that as far as I can tell.
        // so lets play games.
        Item item;
        long size = 0;
        // handle checksum files separately as Artifactory doesn't return metadata for checksums.
        if (path.endsWith(".md5") || path.endsWith(".sha1")) {
            return createCheckSumMetaData(path);
        }

        if (path.endsWith("/")) {
            item = handleFolder(path);
        } else {
            item = handleFile(path);
        }

        ChildArtifact[] children = null;
        Date created = null;

        if (item.isFolder() && item instanceof Folder) {
            Folder folder = (Folder) item;
            children = ChildArtifact.buildFromItemList(folder.getChildren());
            created = folder.getCreated();
        } else if (item instanceof File) {
            created = ((File) item).getCreated();
            size = ((File) item).getSize();
        }

        return new ArtifactMetaData(children /* childArtifacts */, created /* created */, item.getLastModified(),
                        item.getLastUpdated(), item.getModifiedBy(), item.getRepo(), size, item.getUri());
    }

    private ArtifactMetaData createCheckSumMetaData(String filePath) throws ParseException {
        long size = 0;
        if (filePath.endsWith(".md5")) {
            size = 32;
        } else if (filePath.endsWith(".sha1")) {
            size = 40;
        }
        return new ArtifactMetaData(null, null, null, null, null, null, size, null);
    }

    private Item handleFolder(String path) throws ArtifactNotFoundException, IOException {
        ItemHandle itemHandle;
        Item item = null;
        try {
            // we'll default to a folder
            // this is groovy underneath, so we have to pretend we could
            // throw an exception
            couldThrowIOException();

            itemHandle = repository.folder(path);
            // force download which if it's a folder will throw an
            // IOException.
            item = itemHandle.info();
        } catch (IOException e) {
            // oops, it's probably a folder. suffer another rest call.
            itemHandle = repository.file(path);
            try {
                couldThrowIOException();
                item = itemHandle.info();
            } catch (IOException ex) {
                handleIOException(ex);
            }
        }
        return item;
    }

    private Item handleFile(String path) throws ArtifactNotFoundException, IOException {
        ItemHandle itemHandle;
        Item item = null;
        // it could be a folder or a file.
        // we'll try a file first.
        try {
            // this is groovy underneath, so we have to pretend we could
            // throw an exception
            couldThrowIOException();

            itemHandle = repository.file(path);
            // force download which if it's a folder will throw an
            // IOException.
            item = itemHandle.info();
        } catch (IOException e) {
            // oops, it's probably a folder. suffer another rest call.
            itemHandle = repository.folder(path);
            try {
                couldThrowIOException();
                item = itemHandle.info();
            } catch (IOException ex) {
                handleIOException(ex);
            }
        }
        return item;
    }

    private static void handleIOException(final IOException e) throws ArtifactNotFoundException, IOException {
        if (e instanceof org.apache.http.client.HttpResponseException) {
            int statusCode = ((org.apache.http.client.HttpResponseException) e).getStatusCode();
            if (statusCode == 404) {
                throw new ArtifactNotFoundException(e);
            } else {
                // Throw back the error for other status codes. We shouldn't be
                // here if we have 2xx so thats fine.
                throw e;
            }
        } else {
            throw e;
        }
    }

    private static final void couldThrowIOException() throws IOException {
        // we don't throw, this is just to avoid unreachable catch block
        // syndrome.
        // because it's groovy under us.
    }

    public InputStream getArtifactContents(String filePath) throws ArtifactNotFoundException, IOException {
        InputStream in = null;
        try {
            couldThrowIOException();
            in = repository.download(filePath).doDownload();
        } catch (IOException ex) {
            handleIOException(ex);
        }
        return in;
    }

    public void putArtifact(PipedInputStream snk, String filePath, Map<String, Object> properties, AsyncHandler handler)
                    throws ArtifactNotFoundException, IOException {
        try {
            couldThrowIOException();
            final UploadableArtifact upload = repository.upload(filePath, snk);

            if (null != properties && !properties.isEmpty()) {
                for (Entry<String, Object> e : properties.entrySet()) {
                    upload.withProperty(e.getKey(), e.getValue());
                }
            }
            // We can't block here because we need to call outputstream.close() which happens outside of this method. So
            // are using handler to handle it.
            CACHED_THREAD_POOL.submit(getUploader(upload, handler));
        } catch (IOException ex) {
            handleIOException(ex);
        }
    }

    static final class Uploader implements Callable<Void> {
        private final UploadableArtifact upload;
        private final AsyncHandler handler;

        public Uploader(UploadableArtifact upload, AsyncHandler handler) {
            this.upload = upload;
            this.handler = handler;
        }

        @Override
        public Void call() throws Exception {
            // Catch the exceptions and signal through the AsyncHandler handler since throwing Exception can not be
            // caught.
            try {
                upload.doUpload();
                handler.onCompleted();
            } catch (Throwable e) {
                handler.onThrowable(e);
            }
            return null;
        }
    }

    protected Uploader getUploader(UploadableArtifact upload, AsyncHandler handler) {
        return new Uploader(upload, handler);
    }
}
