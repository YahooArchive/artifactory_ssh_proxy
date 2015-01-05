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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.common.Closeable;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.util.CloseableUtils;
import org.apache.sshd.common.util.Readable;

/**
 * This should completely kill all forwarding even if enabled.
 */
public class DenyingTcpipForwarder extends CloseableUtils.AbstractInnerCloseable implements TcpipForwarder, IoHandler {

    public DenyingTcpipForwarder(ConnectionService service) {

    }

    //
    // TcpIpForwarder implementation
    //

    @Override
    public synchronized SshdSocketAddress startLocalPortForwarding(SshdSocketAddress local, SshdSocketAddress remote)
                    throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public synchronized void stopLocalPortForwarding(SshdSocketAddress local) throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");

    }

    @Override
    public synchronized SshdSocketAddress startRemotePortForwarding(SshdSocketAddress remote, SshdSocketAddress local)
                    throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public synchronized void stopRemotePortForwarding(SshdSocketAddress remote) throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public synchronized SshdSocketAddress getForwardedPort(int remotePort) {
        return null;
    }

    @Override
    public synchronized SshdSocketAddress localPortForwardingRequested(SshdSocketAddress local) throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public synchronized void localPortForwardingCancelled(SshdSocketAddress local) throws IOException {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public synchronized void close() {
        close(true);
    }

    @Override
    protected synchronized Closeable getInnerCloseable() {
        return new Closeable() {
            @Override
            public boolean isClosing() {
                return false;
            }

            @Override
            public boolean isClosed() {
                return true;
            }

            @Override
            public CloseFuture close(boolean immediately) {
                return new CloseFuture() {

                    @Override
                    public CloseFuture removeListener(SshFutureListener<CloseFuture> listener) {
                        return this;
                    }

                    @Override
                    public boolean isDone() {
                        return true;
                    }

                    @Override
                    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
                        return true;
                    }

                    @Override
                    public boolean awaitUninterruptibly(long timeoutMillis) {
                        return true;
                    }

                    @Override
                    public CloseFuture awaitUninterruptibly() {
                        return this;
                    }

                    @Override
                    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
                        return true;
                    }

                    @Override
                    public boolean await(long timeoutMillis) throws InterruptedException {
                        return true;
                    }

                    @Override
                    public CloseFuture await() throws InterruptedException {
                        return this;
                    }

                    @Override
                    public CloseFuture addListener(SshFutureListener<CloseFuture> listener) {
                        return this;
                    }

                    @Override
                    public void setClosed() {

                    }

                    @Override
                    public boolean isClosed() {
                        return true;
                    }
                };
            }
        };
    }

    //
    // IoHandler implementation
    //

    @Override
    public void sessionCreated(final IoSession session) throws Exception {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public void messageReceived(IoSession session, Readable message) throws Exception {
        throw new SshException("Tcpip forwarding request denied by server");
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
        session.close(false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
