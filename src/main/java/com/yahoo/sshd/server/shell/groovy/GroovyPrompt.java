package com.yahoo.sshd.server.shell.groovy;

import groovy.lang.Closure;

public class GroovyPrompt extends Closure<String> {
    private static final long serialVersionUID = 1L;

    public GroovyPrompt(Object owner) {
        super(owner);
    }

    public String doCall() {
        return "groovy => ";
    }

}
