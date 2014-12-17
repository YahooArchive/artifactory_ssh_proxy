package com.yahoo.sshd.server;

import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.NamedFactory;

import com.yahoo.sshd.server.settings.SshdProxySettings;

public class TestCiphers {
    public static void main(String[] args) throws Exception {
        List<NamedFactory<Cipher>> list = SshdProxySettings.createCipherFactoryList(Collections.<String>emptyList());
        System.err.println("length: " + list.size());
    }
}
