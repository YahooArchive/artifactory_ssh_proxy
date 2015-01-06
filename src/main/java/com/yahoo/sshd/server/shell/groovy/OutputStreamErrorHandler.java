package com.yahoo.sshd.server.shell.groovy;

import java.io.OutputStream;
import java.io.PrintStream;

import groovy.lang.Closure;

public class OutputStreamErrorHandler extends Closure<Void> {
    private static final long serialVersionUID = 1L;

    private final OutputStream err;

    public OutputStreamErrorHandler(Object owner, OutputStream err) {
        super(owner);
        this.err = err;
    }

    public Void doCall(Object o) {
        if (!(o instanceof Throwable) || null == err) {
            return null;
        }

        try (PrintStream ps = new PrintStream(err)) {
            ((Throwable) o).printStackTrace(ps);
        }

        return null;
    }

}
