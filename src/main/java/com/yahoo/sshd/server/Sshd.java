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
package com.yahoo.sshd.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.settings.SshdConfigurationException;

public class Sshd implements Daemon, Runnable {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Sshd.class);

    /*
     * ================================= Main class implementation =================================
     */

    /**
     * used when we are launched from jsvc
     */
    private String[] args;

    private SshServerWrapper sshServer;

    /**
     * constructor used by jsvc
     */
    public Sshd() {

    }

    /**
     * constructor used by main.
     * 
     * @param args
     * @throws SshdConfigurationException
     */
    public Sshd(@Nonnull final String[] args) throws SshdConfigurationException {
        this.args = args;
        setup();
    }

    /**
     * We have to set things up in setup, because if we are called by jsvc, it creates us then call start, which is
     * where we call setup.
     * 
     * @throws SshdConfigurationException
     */
    protected void setup() throws SshdConfigurationException {
        this.sshServer = new SshServerWrapper(this.args);
    }

    public static void main(String[] args) throws Exception {
        Sshd sshd = new Sshd(args);
        sshd.run();

        sshd.getLatch().await();
    }

    private CountDownLatch getLatch() {
        return sshServer.getLatch();
    }

    @Override
    public void run() {
        sshServer.run();
    }

    /**
     * Initialize this <code>Daemon</code> instance.
     * <p>
     * This method gets called once the JVM process is created and the <code>Daemon</code> instance is created thru its
     * empty public constructor.
     * </p>
     * <p>
     * Under certain operating systems (typically Unix based operating systems) and if the native invocation framework
     * is configured to do so, this method might be called with <i>super-user</i> privileges.
     * </p>
     * <p>
     * For example, it might be wise to create <code>ServerSocket</code> instances within the scope of this method, and
     * perform all operations requiring <i>super-user</i> privileges in the underlying operating system.
     * </p>
     * <p>
     * Apart from set up and allocation of native resources, this method must not start the actual operation of the
     * <code>Daemon</code> (such as starting threads calling the <code>ServerSocket.accept()</code> method) as this
     * would impose some serious security hazards. The start of operation must be performed in the <code>start()</code>
     * method.
     * </p>
     * 
     * @param context A <code>DaemonContext</code> object used to communicate with the container.
     * @exception DaemonInitException An exception that prevented initialization where you want to display a nice
     *            message to the user, rather than a stack trace.
     * @exception Exception Any exception preventing a successful initialization.
     */
    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        // save our arguments
        this.args = arg0.getArguments();
    }

    /**
     * Start the operation of this <code>Daemon</code> instance. This method is to be invoked by the environment after
     * the init() method has been successfully invoked and possibly the security level of the JVM has been dropped.
     * Implementors of this method are free to start any number of threads, but need to return control after having done
     * that to enable invocation of the stop()-method.
     */
    @Override
    public void start() throws Exception {
        // now that we have args, and are ready to start, setup the settings
        setup();
        new Thread(this).start();
    }

    /**
     * Stop the operation of this <code>Daemon</code> instance. Note that the proper place to free any allocated
     * resources such as sockets or file descriptors is in the destroy method, as the container may restart the Daemon
     * by calling start() after stop().
     * 
     * @throws InterruptedException
     */
    @Override
    public void stop() throws InterruptedException, IOException {
        sshServer.close();
    }

    /**
     * Free any resources allocated by this daemon such as file descriptors or sockets. This method gets called by the
     * container after stop() has been called, before the JVM exits. The Daemon can not be restarted after this method
     * has been called without a new call to the init() method.
     */
    @Override
    public void destroy() {

    }
}
