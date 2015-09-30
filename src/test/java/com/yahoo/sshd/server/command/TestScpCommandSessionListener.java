package com.yahoo.sshd.server.command;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test the ScpCommandSessionListener
 *
 * Created by adam701 on 9/30/15.
 *
 */
public class TestScpCommandSessionListener {

    @Test
    public void testSessionCloseInterruptThread() throws InterruptedException {
        RunnableWithInterruptedFlag blockedTask = new RunnableWithInterruptedFlag();

        Thread thread = Executors.defaultThreadFactory().newThread(blockedTask);
        thread.start();

        ScpCommandSessionListener scpCommandSessionListener = new ScpCommandSessionListener(thread);
        scpCommandSessionListener.sessionClosed(null);
        thread.join(100000);

        Assert.assertEquals(blockedTask.interrupt, true, "Session close event should interrupt the thread.");
    }

    private class RunnableWithInterruptedFlag implements Runnable {

        boolean interrupt = false;

        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                this.interrupt = true;
            }
        }

    }

}
