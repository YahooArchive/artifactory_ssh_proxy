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

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.UploadableArtifact;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    protected final static int retryCount=5;
    
    // FIXME: need to inject NingRequestImpl
    public JFrogArtifactoryClientHelper(final ArtifactoryInformation afInfo, final String repositoryName) {
        this.repositoryName = repositoryName;
        this.afClient = createAfClient(afInfo);
        this.repository = afClient.repository(this.repositoryName);
    }

    protected static int getRetryCount(){
        return retryCount;
    }
    
    protected Artifactory createAfClient(final ArtifactoryInformation afInfo) {
        return org.jfrog.artifactory.client.ning.ArtifactoryClient.create(afInfo.getArtifactoryUrl(),
                        afInfo.getArtifactoryUsername(), afInfo.getArtifactoryPassword(), afInfo.createNingRequest());
    }

    public ArtifactMetaData getArtifact(String path) throws ParseException, IOException, ArtifactNotFoundException {
        // handle checksum files separately as Artifactory doesn't return metadata for checksums.
        if (path.endsWith(".md5") || path.endsWith(".sha1") || path.endsWith(".sha256")) {
            return createCheckSumMetaData(path);
        }

        for (int i = 1; i < retryCount + 1; i++){
            try {
                couldThrowIOException();

                long size = 0;
                ChildArtifact[] children = null;
                Date created = null;

                Item item;
                ItemHandle itemHandle;

                if (repository.isFolder(path)) {
                    itemHandle = repository.folder(path);
                    item = itemHandle.info();
                    Folder folder = (Folder) item;
                    children = ChildArtifact.buildFromItemList(folder.getChildren());
                    created = folder.getCreated();
                } else {
                    itemHandle = repository.file(path);
                    item = itemHandle.info();
                    created = ((File) item).getCreated();
                    size = ((File) item).getSize();
                }

                return new ArtifactMetaData(children /* childArtifacts */, created /* created */, item.getLastModified(),
                        item.getLastUpdated(), item.getModifiedBy(), item.getRepo(), size, item.getUri());

            } catch (IOException e) {
                handleIOException(e, i);
            }
        }

        // This does not happen because exceptions are re-thrown in the catch block
        return null;
    }

    private ArtifactMetaData createCheckSumMetaData(String filePath) throws ParseException {
        long size = 0;
        if (filePath.endsWith(".md5")) {
            size = 32;
        } else if (filePath.endsWith(".sha1")) {
            size = 40;
        } else if (filePath.endsWith(".sha256")) {
            size = 64;
        }
        return new ArtifactMetaData(null, null, null, null, null, null, size, null);
    }

    private static void handleIOException(final IOException e, final int currentRetry) throws ArtifactNotFoundException, IOException {
        if (e instanceof org.apache.http.client.HttpResponseException) {
            int statusCode = ((org.apache.http.client.HttpResponseException) e).getStatusCode();
            if (statusCode == 404) {
                throw new ArtifactNotFoundException(e);
            } else {
                // Throw back the error for other status codes. We shouldn't be
                // here if we have 2xx so thats fine.
                warnRetryMessage(e, currentRetry);
            }
        } else {
            warnRetryMessage(e, currentRetry);
        }
    }
    
    private static final void warnRetryMessage(final IOException e, final int currentRetry) throws IOException{
        if (currentRetry == retryCount){
            throw e;
        }else{
            LOGGER.warn(e + " retrying " + currentRetry + "/" +  retryCount );
        }
    }

    private static final void couldThrowIOException() throws IOException {
        // we don't throw, this is just to avoid unreachable catch block
        // syndrome.
        // because it's groovy under us.
    }

    public InputStream getArtifactContents(String filePath) throws ArtifactNotFoundException, IOException {
        for (int i = 1; i < retryCount + 1; i++){
            try {
                couldThrowIOException();
                return repository.download(filePath).doDownload();
            } catch (IOException ex) {
                handleIOException(ex, i);
            }
        }
        return null;
    }

    public Future<Void> putArtifact(PipedInputStream snk, String filePath, Map<String, Object> properties,
                                    AsyncHandler handler)
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
            return CACHED_THREAD_POOL.submit(getUploader(upload, handler));
        } catch (IOException ex) {
            handleIOException(ex, retryCount);

            // This statement is not reachable since the handleIOException will rethrow the exception.
            return null;
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
