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
package com.yahoo.sshd.server.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshRequestLogListener implements SessionListener, SshRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshRequestLogListener.class);
    private SshRequestLog requestLog;
    private List<SshRequestInfo> sshRequestList = new ArrayList<SshRequestInfo>();

    public SshRequestLogListener(SshRequestLog log) {
        this.requestLog = log;
    }

    public void setSshRequestLog(SshRequestLog log) {
        this.requestLog = log;
    }

    public void registerSession(ServerSession session) {
        session.addListener(this);
    }

    @Override
    public void sessionCreated(Session session) {
        // this may not be called if the listener is created after the session
        // has been created
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("session " + session.toString() + "gets started.");
        }
    }

    @Override
    public void sessionClosed(Session session) {
        for (SshRequestInfo request : sshRequestList) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("logging the ssh request info " + request.toString());
            }
            requestLog.log(request);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("session" + session.toString() + " closed.");
        }
    }

    @Override
    public void handleRequest(final SshRequestInfo requestInfo) {
        if (null == requestInfo) {
            LOGGER.warn("requestInfo is null somehow, no logging");
            return;
        }
        sshRequestList.add(requestInfo);
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("session " + session.toString() + " Event: " + event.toString());
        }
    }

}
