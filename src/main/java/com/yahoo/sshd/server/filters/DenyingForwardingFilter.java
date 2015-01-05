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
package com.yahoo.sshd.server.filters;


import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists to deny all sorts of forwarding.
 * 
 * we don't allow X11, agent, forwarding, or listening.
 * 
 * @author areese
 * 
 */
public class DenyingForwardingFilter implements ForwardingFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DenyingForwardingFilter.class);

    public boolean canForwardAgent(ServerSession session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Agent forwarding requested for {}", session);
        }

        return false;
    }

    @Override
    public boolean canForwardX11(Session session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("X11 forwarding requested for {}", session);
        }

        return false;
    }

    @Override
    public boolean canListen(SshdSocketAddress socketAddress, Session session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Listen forwarding requested from {} for {}", socketAddress, session);
        }

        return false;
    }

    @Override
    public boolean canConnect(SshdSocketAddress socketAddress, Session session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connect forwarding requested from {} for {}", socketAddress, session);
        }

        return false;
    }

    @Override
    public boolean canForwardAgent(Session session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Agent forwarding requested for {}", session);
        }

        return false;
    }
}
