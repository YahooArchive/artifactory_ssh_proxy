/*
 * Copyright 2015 Yahoo! Inc.
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
package com.yahoo.sshd.common.forward;

import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.TcpipForwarderFactory;
import org.apache.sshd.common.session.ConnectionService;

/**
 * A denying {@link TcpipForwarderFactory} implementation.
 * 
 * @see DenyingTcpipForwarder
 */
public class DenyingTcpipForwarderFactory implements TcpipForwarderFactory {
    @Override
    public TcpipForwarder create(ConnectionService service) {
        return new DenyingTcpipForwarder(service);
    }
}
