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
package com.yahoo.sshd.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUtils {
    /**
     * The default thread factory
     */
    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public DefaultThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name + "-pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

            t.setDaemon(true);

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }

    private static final ExecutorService CACHED_THREAD_POOL = new ThreadPoolExecutor(20, 100, 60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadUtils.DefaultThreadFactory("SSH Proxy Thread Pool"));

    public static ExecutorService cachedThreadPool() {
        return CACHED_THREAD_POOL;
    }

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(20,
                    new ThreadUtils.DefaultThreadFactory("SSH Proxy Thread Pool"));

    public static ScheduledExecutorService scheduledExecutorServer() {
        return SCHEDULED_EXECUTOR_SERVICE;
    }

    private static final ExecutorService EXTERNALS_EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 5, 60L,
                    TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadUtils.DefaultThreadFactory(
                                    "SSH Externals Thread Pool"));

    public static ExecutorService externalsThreadPool() {
        return EXTERNALS_EXECUTOR_SERVICE;
    }

}
