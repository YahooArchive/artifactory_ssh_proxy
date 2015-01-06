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
import java.util.Locale;

import org.eclipse.jetty.server.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.utils.ThreadSafeSimpleDateFormat;


/**
 * Base implementation of the {@link RequestLog} outputs logs in the pseudo-standard NCSA common log format.
 * Configuration options allow a choice between the standard Common Log Format (as used in the 3 log format) and the
 * Combined Log Format (single log format). This log format can be output by most web servers, and almost all web log
 * analysis software can understand these formats.
 */
public abstract class AbstractNCSARequestLog implements SshRequestLog {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractNCSARequestLog.class);

    private static ThreadLocal<StringBuilder> buffers = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder(256);
        }
    };

    private static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";

    private boolean logLatency = true;
    private Locale logLocale = Locale.getDefault();
    private String logTimeZone = "GMT";

    protected static final ThreadSafeSimpleDateFormat DATE_FORMATTER = new ThreadSafeSimpleDateFormat(LOG_DATE_FORMAT);

    /* ------------------------------------------------------------ */

    /**
     * Is logging enabled
     */
    protected abstract boolean isEnabled();

    /* ------------------------------------------------------------ */

    /**
     * Write requestEntry out. (to disk or slf4j log)
     */
    public abstract void write(String requestEntry) throws IOException;

    /* ------------------------------------------------------------ */

    /**
     * Writes the request and response information to the output stream.
     * 
     * @see org.eclipse.jetty.server.RequestLog#log(org.eclipse.jetty.server.Request, org.eclipse.jetty.server.Response)
     */
    @Override
    public void log(final SshRequestInfo request) {
        if (null == request) {
            LOG.warn("ssh request info obj null");
            return;
        }
        try {
            if (!isEnabled()) {
                return;
            }

            StringBuilder buf = buffers.get();
            buf.setLength(0);

            String addr = request.getRemoteAddr();

            buf.append(addr);
            buf.append(" - ");

            buf.append(request.getUserName());

            // request timestamp
            buf.append(" [");
            buf.append(DATE_FORMATTER.formatLongToString(request.getStartTimestamp()));
            buf.append("] \"");

            buf.append(request.getMethod());
            buf.append(' ');
            buf.append(request.getRepoName());
            buf.append(':');
            buf.append(request.getRequestPath());
            buf.append(' ');
            buf.append(request.getProtocol());
            buf.append("\" ");

            int status = request.getStatus();
            if (status <= 0) {
                status = 404;
            }

            buf.append((char) ('0' + ((status / 100) % 10)));
            buf.append((char) ('0' + ((status / 10) % 10)));
            buf.append((char) ('0' + (status % 10)));

            int exitValue = request.getExitValue();
            buf.append(' ');
            buf.append(exitValue);

            long responseLength = request.getRequstContentSize();
            if (responseLength >= 0) {
                buf.append(' ');
                if (responseLength > 99999) {
                    buf.append(responseLength);
                } else {
                    if (responseLength > 9999) {
                        buf.append((char) ('0' + ((responseLength / 10000) % 10)));
                    }
                    if (responseLength > 999) {
                        buf.append((char) ('0' + ((responseLength / 1000) % 10)));
                    }
                    if (responseLength > 99) {
                        buf.append((char) ('0' + ((responseLength / 100) % 10)));
                    }
                    if (responseLength > 9) {
                        buf.append((char) ('0' + ((responseLength / 10) % 10)));
                    }
                    buf.append((char) ('0' + (responseLength) % 10));
                }
                buf.append(' ');
            } else {
                buf.append(" - ");
            }


            if (this.logLatency) {
                long now = System.currentTimeMillis();

                if (this.logLatency) {
                    buf.append(' ');
                    buf.append(now - request.getStartTimestamp());
                }
            }

            String log = buf.toString();
            write(log);
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    /**
     * Controls logging of request processing time.
     * 
     * @param logLatency true - request processing time will be logged false - request processing time will not be
     *        logged
     */
    public void setLogLatency(boolean logLatency) {
        this.logLatency = logLatency;
    }

    /**
     * Retrieve log request processing time flag.
     * 
     * @return value of the flag
     */
    public boolean getLogLatency() {
        return this.logLatency;
    }

    // FIXME, these never could have worked, because the formatter wasn't re-created after this was set.
    /**
     * Set the timestamp format for request log entries in the file. If this is not set, the pre-formated request
     * timestamp is used.
     * 
     * @param format timestamp format string
     */
    /*
     * public void setLogDateFormat(String format) { LOG_DATE_FORMAT = format; }
     */

    /**
     * Retrieve the timestamp format string for request log entries.
     * 
     * @return timestamp format string.
     */
    /*
     * public String getLogDateFormat() { return LOG_DATE_FORMAT; }
     */

    /**
     * Set the locale of the request log.
     * 
     * @param logLocale locale object
     */
    public void setLogLocale(Locale logLocale) {
        this.logLocale = logLocale;
    }

    /**
     * Retrieve the locale of the request log.
     * 
     * @return locale object
     */
    public Locale getLogLocale() {
        return this.logLocale;
    }

    /**
     * Set the timezone of the request log.
     * 
     * @param tz timezone string
     */
    public void setLogTimeZone(String tz) {
        this.logTimeZone = tz;
    }

    /**
     * Retrieve the timezone of the request log.
     * 
     * @return timezone string
     */
    public String getLogTimeZone() {
        return this.logTimeZone;
    }
}
