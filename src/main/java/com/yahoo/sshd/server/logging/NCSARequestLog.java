//
// ========================================================================
// Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
//
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
//
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
//
// You may elect to redistribute this code under either of these licenses.
// ========================================================================
//
/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */

package com.yahoo.sshd.server.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link RequestLog} implementation outputs logs in the pseudo-standard NCSA common log format. Configuration
 * options allow a choice between the standard Common Log Format (as used in the 3 log format) and the Combined Log
 * Format (single log format). This log format can be output by most web servers, and almost all web log analysis
 * software can understand these formats.
 */

public class NCSARequestLog extends AbstractNCSARequestLog implements SshRequestLog {

    protected static final Logger LOG = LoggerFactory.getLogger(NCSARequestLog.class);
    protected static final String NEW_LINE_SEP = System.lineSeparator();
    private String filename;
    private boolean append;
    private int retainDays;
    private boolean closeOut;
    private String filenameDateFormat = null;
    private transient OutputStream out;
    private transient OutputStream fileOut;
    private transient Writer writer;

    /* ------------------------------------------------------------ */
    /**
     * Create request log object with default settings.
     */
    NCSARequestLog() {
        this.append = true;
        this.retainDays = 31;
    }

    /* ------------------------------------------------------------ */
    /**
     * Create request log object with specified output file name.
     * 
     * @param filename the file name for the request log. This may be in the format expected by
     *        {@link RolloverFileOutputStream}
     */
    NCSARequestLog(String filename) {
        this.append = true;
        this.retainDays = 31;
        setFilename(filename);
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the output file name of the request log. The file name may be in the format expected by
     * {@link RolloverFileOutputStream}.
     * 
     * @param filename file name of the request log
     * 
     */
    public void setFilename(String filename) {
        if (filename != null) {
            filename = filename.trim();
            if (filename.length() == 0) {
                filename = null;
            }
        }
        this.filename = filename;
    }

    /* ------------------------------------------------------------ */
    /**
     * Retrieve the output file name of the request log.
     * 
     * @return file name of the request log
     */
    public String getFilename() {
        return this.filename;
    }

    /* ------------------------------------------------------------ */
    /**
     * Retrieve the file name of the request log with the expanded date wildcard if the output is written to the disk
     * using {@link RolloverFileOutputStream}.
     * 
     * @return file name of the request log, or null if not applicable
     */
    public String getDatedFilename() {
        if (this.fileOut instanceof ConfigurableRolloverOuputStream) {
            return ((ConfigurableRolloverOuputStream) this.fileOut).getDatedFilename();
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    @Override
    protected boolean isEnabled() {
        return (this.fileOut != null);
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the number of days before rotated log files are deleted.
     * 
     * @param retainDays number of days to keep a log file
     */
    public void setRetainDays(int retainDays) {
        this.retainDays = retainDays;
    }

    /* ------------------------------------------------------------ */
    /**
     * Retrieve the number of days before rotated log files are deleted.
     * 
     * @return number of days to keep a log file
     */
    public int getRetainDays() {
        return this.retainDays;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set append to log flag.
     * 
     * @param append true - request log file will be appended after restart, false - request log file will be
     *        overwritten after restart
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /* ------------------------------------------------------------ */
    /**
     * Retrieve append to log flag.
     * 
     * @return value of the flag
     */
    public boolean isAppend() {
        return this.append;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the log file name date format.
     * 
     * @see RolloverFileOutputStream#RolloverFileOutputStream(String, boolean, int, TimeZone, String, String)
     * 
     * @param logFileDateFormat format string that is passed to {@link RolloverFileOutputStream}
     */
    public void setFilenameDateFormat(String logFileDateFormat) {
        this.filenameDateFormat = logFileDateFormat;
    }

    /* ------------------------------------------------------------ */
    /**
     * Retrieve the file name date format string.
     * 
     * @return the log File Date Format
     */
    public String getFilenameDateFormat() {
        return this.filenameDateFormat;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void write(String requestEntry) throws IOException {
        synchronized (this) {
            if (this.writer == null) {
                return;
            }

            this.writer.write(requestEntry);
            this.writer.write(NEW_LINE_SEP);
            this.writer.flush();
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Set up request logging and open log file.
     * 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
     */
    @Override
    public synchronized void setup() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setting up NCSARequestLog");
        }

        if (this.filename != null) {
            this.fileOut =
                            new ConfigurableRolloverOuputStream(this.filename, this.append, this.retainDays,
                                            TimeZone.getTimeZone(getLogTimeZone()));
            closeOut = true;
            LOG.info("Opened " + getDatedFilename());
        } else {
            this.fileOut = System.err;
        }

        this.out = this.fileOut;

        synchronized (this) {
            this.writer = new OutputStreamWriter(this.out);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Close the log file and perform cleanup.
     * 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStop()
     */
    @Override
    public void cleanup() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("cleaning up NCSARequestLog");
        }

        synchronized (this) {
            try {
                if (this.writer != null) {
                    this.writer.flush();
                }
            } catch (IOException e) {
                LOG.warn(e.getMessage());
            }

            if (this.out != null && closeOut) {
                try {
                    this.out.close();
                } catch (IOException e) {
                    LOG.warn(e.getMessage());
                }
            }

            this.out = null;
            this.fileOut = null;
            closeOut = false;
            this.writer = null;
        }
    }
}
