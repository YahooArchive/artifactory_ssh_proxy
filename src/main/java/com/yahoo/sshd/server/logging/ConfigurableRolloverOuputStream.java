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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigurableRolloverOuputStream
 * 
 * This output stream puts content in a file that is rolled over every 24 hours. The filename must include the string
 * "yyyy_mm_dd", which is replaced with the actual date when creating and rolling over the file.
 * 
 * Old files are retained for a number of days before being deleted.
 * 
 * 
 */
public class ConfigurableRolloverOuputStream extends FilterOutputStream {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableRolloverOuputStream.class);
    private static Timer ROLLOVER_TIMER;

    final static String ROLLOVER_FILE_DATE_FORMAT = "yyyy_MM_dd";
    final static String ROLLOVER_FILE_BACKUP_FORMAT = "HHmmssSSS";
    final static int ROLLOVER_FILE_RETAIN_DAYS = 31;

    private RollTask rollTask;
    private String backupFormat;
    private SimpleDateFormat fileBackupFormat;

    private String dateFormat;
    private SimpleDateFormat fileDateFormat;

    private String filename;
    private File file;
    private boolean append;
    private int retainDays;


    /* ------------------------------------------------------------ */
    /**
     * @param filename The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
     *        creating and rolling over the file.
     * @throws IOException
     */
    public ConfigurableRolloverOuputStream(String filename) throws IOException {
        this(filename, true, ROLLOVER_FILE_RETAIN_DAYS);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param filename The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
     *        creating and rolling over the file.
     * @param append If true, existing files will be appended to.
     * @throws IOException
     */
    public ConfigurableRolloverOuputStream(String filename, boolean append) throws IOException {
        this(filename, append, ROLLOVER_FILE_RETAIN_DAYS);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param filename The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
     *        creating and rolling over the file.
     * @param append If true, existing files will be appended to.
     * @param retainDays The number of days to retain files before deleting them. 0 to retain forever.
     * @throws IOException
     */
    public ConfigurableRolloverOuputStream(String filename, boolean append, int retainDays) throws IOException {
        this(filename, append, retainDays, TimeZone.getDefault());
    }

    /* ------------------------------------------------------------ */
    /**
     * @param filename The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
     *        creating and rolling over the file.
     * @param append If true, existing files will be appended to.
     * @param retainDays The number of days to retain files before deleting them. 0 to retain forever.
     * @throws IOException
     */
    public ConfigurableRolloverOuputStream(String filename, boolean append, int retainDays, TimeZone zone)
                    throws IOException {

        this(filename, append, retainDays, zone, 1, TimeUnit.DAYS, null, null);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param filename The filename must include the string "yyyy_mm_dd", which is replaced with the actual date when
     *        creating and rolling over the file.
     * @param append If true, existing files will be appended to.
     * @param retainDays The number of days to retain files before deleting them. 0 to retain forever.
     * @param dateFormat The format for the date file substitution. The default is "yyyy_MM_dd".
     * @param backupFormat The format for the file extension of backup files. The default is "HHmmssSSS".
     * @throws IOException
     */
    public ConfigurableRolloverOuputStream(String filename, boolean append, int retainDays, TimeZone zone, long delay,
                    TimeUnit sourceUnit, String dateFormat, String backupFormat) throws IOException {
        super(null);

        if (dateFormat == null) {
            dateFormat = ROLLOVER_FILE_DATE_FORMAT;
        }

        this.dateFormat = dateFormat;
        this.fileDateFormat = new SimpleDateFormat(this.dateFormat);

        if (backupFormat == null) {
            backupFormat = ROLLOVER_FILE_BACKUP_FORMAT;
        }

        this.backupFormat = backupFormat;
        this.fileBackupFormat = new SimpleDateFormat(this.backupFormat);

        this.fileBackupFormat.setTimeZone(zone);
        this.fileDateFormat.setTimeZone(zone);

        if (filename != null) {
            filename = filename.trim();
            if (filename.length() == 0) {
                filename = null;
            }
        }

        if (null == filename) {
            throw new IllegalArgumentException("Invalid filename: null");
        }

        if (!filename.contains(dateFormat)) {
            // oops, no date format
            String props = System.getProperty("sshd.logs");
            String newfilename = props + "/access." + dateFormat + ".log";
            new IllegalArgumentException("Invalid filename: filename " + filename + " does not contain dateformat: "
                            + this.dateFormat + " and can't be rolled, defaulting to: " + newfilename)
                            .printStackTrace();
            filename = newfilename;
        }

        this.filename = filename;
        this.append = append;
        this.retainDays = retainDays;
        setFile();

        synchronized (ConfigurableRolloverOuputStream.class) {
            if (ROLLOVER_TIMER == null) {
                ROLLOVER_TIMER = new Timer(ConfigurableRolloverOuputStream.class.getName(), true);
            }

            // bug 6816241, start at HOUR etc, instead of now + time.

            this.rollTask = new RollTask();

            final long now = System.currentTimeMillis();
            final long startAfter = getStartAfter(now, sourceUnit);
            final long delayMs = TimeUnit.MILLISECONDS.convert(delay, sourceUnit);

            LOGGER.info("Scheduling for " + this.filename + " start: " + startAfter + "MS "
                            + sourceUnit.convert(startAfter, TimeUnit.MILLISECONDS) + " delay: " + delayMs + "MS "
                            + +sourceUnit.convert(delayMs, TimeUnit.MILLISECONDS));
            ROLLOVER_TIMER.scheduleAtFixedRate(this.rollTask, startAfter, delayMs);
        }
    }

    static long getStartAfter(final long now, final TimeUnit sourceUnit) {
        final long nowInSourceUnit = sourceUnit.convert(now, TimeUnit.MILLISECONDS);
        final long nextSourceUnit = nowInSourceUnit + 1;
        final long startAt = TimeUnit.MILLISECONDS.convert(nextSourceUnit, sourceUnit);

        return startAt - now;
    }

    /* ------------------------------------------------------------ */
    public String getFilename() {
        return this.filename;
    }

    /* ------------------------------------------------------------ */
    public String getDatedFilename() {
        if (this.file == null) {
            return null;
        }
        return this.file.toString();
    }

    /* ------------------------------------------------------------ */
    public int getRetainDays() {
        return this.retainDays;
    }

    /* ------------------------------------------------------------ */
    private synchronized void setFile() throws IOException {
        // Check directory
        File file = new File(this.filename);
        this.filename = file.getCanonicalPath();

        file = new File(this.filename);
        File dir = new File(file.getParent());

        if (!dir.isDirectory() || !dir.canWrite()) {
            throw new IOException("Cannot write log directory " + dir);
        }

        Date now = new Date();

        // Is this a rollover file?
        final String filename = file.getName();
        int i = filename.indexOf(this.dateFormat);
        if (i >= 0) {
            file =
                            new File(dir, filename.substring(0, i) + this.fileDateFormat.format(now)
                                            + filename.substring(i + this.dateFormat.length()));
        } else {
            new Exception("Unable to find format: '" + this.dateFormat + "' in filename: " + this.filename + " i was "
                            + i).printStackTrace();
        }

        if (file.exists() && !file.canWrite()) {
            throw new IOException("Cannot write log file " + file);
        }

        String symlinkFilename = filename.substring(0, i);
        if (symlinkFilename.endsWith(".")) {
            symlinkFilename = symlinkFilename.substring(0, symlinkFilename.length() - 1);
        }

        final File symlink = new File(dir, symlinkFilename);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking symlink " + symlink);
        }


        try {
            final Path symlinkPath = symlink.toPath();
            // we need to remove and create the old symlink.
            if (symlink.exists()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removing symlink " + symlink);
                }
                Files.delete(symlinkPath);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating symlink" + symlinkPath + " to " + file);
            }

            Files.createSymbolicLink(symlinkPath, file.toPath());
        } catch (IOException e) {
            LOGGER.warn("Unable to create symlink " + symlink + " to " + file, e);
        }

        // Do we need to change the output stream?
        if (null == out || !file.equals(this.file)) {
            // Yep
            this.file = file;
            if (!this.append && file.exists()) {
                file.renameTo(new File(file.toString() + "." + this.fileBackupFormat.format(now)));
            }

            OutputStream oldOut = out;
            out = new FileOutputStream(file.toString(), this.append);

            if (null != oldOut) {
                oldOut.close();
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Opened " + this.file);
            }
        }
    }

    /* ------------------------------------------------------------ */
    private void removeOldFiles() {
        if (this.retainDays > 0) {
            long now = System.currentTimeMillis();

            File file = new File(this.filename);
            File dir = new File(file.getParent());
            String fn = file.getName();
            int s = fn.indexOf(this.dateFormat);
            if (s < 0) {
                new Exception("Unable to find format: '" + this.dateFormat + "' in filename: " + this.filename
                                + " i was " + s).printStackTrace();
                return;
            }
            String prefix = fn.substring(0, s);
            String suffix = fn.substring(s + this.dateFormat.length());

            String[] logList = dir.list();
            for (int i = 0; i < logList.length; i++) {
                fn = logList[i];
                if (fn.startsWith(prefix) && fn.indexOf(suffix, prefix.length()) >= 0) {
                    File f = new File(dir, fn);
                    long date = f.lastModified();
                    if ((TimeUnit.DAYS.convert(now - date, TimeUnit.MILLISECONDS)) > this.retainDays) {
                        f.delete();
                    }
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    @Override
    public void write(byte[] buf) throws IOException {
        out.write(buf);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        out.write(buf, off, len);
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    @Override
    public void close() throws IOException {
        synchronized (ConfigurableRolloverOuputStream.class) {
            try {
                super.close();
            } finally {
                out = null;
                this.file = null;
            }

            rollTask.cancel();
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class RollTask extends TimerTask {
        @Override
        public void run() {
            try {
                ConfigurableRolloverOuputStream.this.setFile();
                ConfigurableRolloverOuputStream.this.removeOldFiles();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Rolled over");
                }
            } catch (IOException e) {
                LOGGER.warn("failed to rollover", e);
            }
        }
    }
}
