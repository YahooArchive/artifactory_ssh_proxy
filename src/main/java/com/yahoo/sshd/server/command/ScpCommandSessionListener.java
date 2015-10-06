package com.yahoo.sshd.server.command;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to monitor the session close event for a ScpCommand Object so that it can clean up the ScpCommand
 * when the session is closed.
 *
 * @author adam701 on 9/29/15.
 */
public class ScpCommandSessionListener implements SessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScpCommandSessionListener.class);
    private final Thread threadRunningScpCommand;

    public ScpCommandSessionListener(final Thread threadRunningScpCommand) {
        this.threadRunningScpCommand = threadRunningScpCommand;
    }

    public void registerSession(ServerSession session) {
        session.addListener(this);
    }

    @Override
    public void sessionCreated(Session session) {
    }

    @Override
    public void sessionEvent(Session session, Event event) {
    }

    @Override
    public void sessionClosed(Session session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Session is closed. Try to interrupt the downloading thread if it is still blocked.");
        }

        if (null != this.threadRunningScpCommand) {
            this.threadRunningScpCommand.interrupt();
        }
    }
}
