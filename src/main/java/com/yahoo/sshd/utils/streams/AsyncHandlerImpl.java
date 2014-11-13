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

import java.util.concurrent.CountDownLatch;

public final class AsyncHandlerImpl implements AsyncHandler {
    private final CountDownLatch responseArrived;
    private Throwable responseThrowable = null;

    public AsyncHandlerImpl(final CountDownLatch responseArrived) {
        this.responseArrived = responseArrived;
    }

    @Override
    public void onCompleted() throws Exception {
        responseArrived.countDown();
    }

    @Override
    public void onThrowable(Throwable t) {
        responseThrowable = t;
        responseArrived.countDown();
    }

    @Override
    public Throwable getThrowable() {
        return this.responseThrowable;
    }
}
