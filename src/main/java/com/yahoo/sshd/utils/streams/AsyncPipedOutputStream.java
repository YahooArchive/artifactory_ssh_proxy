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
package com.yahoo.sshd.utils.streams;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author charlesk
 * 
 */
public class AsyncPipedOutputStream extends PipedOutputStream {
    private final CountDownLatch responseArrived = new CountDownLatch(1);
    private final AsyncHandlerImpl asyncHandlerImpl = new AsyncHandlerImpl(responseArrived);

    public AsyncPipedOutputStream(PipedInputStream pipedInputStream) throws IOException {
        super(pipedInputStream);
    }

    /**
     * close method will wait until count down latch reaches 0
     */
    @Override
    public void close() throws IOException {
        try {
            // call close here first so that input stream can be read.
            super.close();
            // block here until countdownlatch gets to 0 through AsyncHandlerImpl.
            responseArrived.await();
        } catch (InterruptedException e) {
            throw new IOException("InterruptedException while waiting for the response to arrive.");
        }
        Throwable responseThrowable = asyncHandlerImpl.getThrowable();
        if (responseThrowable != null) {
            // we got exception throw it.
            throw new IOException(responseThrowable);
        }
    }

    /**
     * 
     * @return AsyncHandler implementation which has been initialized with CountDownLatch.
     */
    public AsyncHandler getAsyncHandler() {
        return this.asyncHandlerImpl;
    }
}
