/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/* Some portions of this code are Copyright (c) 2014, Yahoo! Inc. All rights reserved. */
package org.apache.sshd.server.keyprovider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.util.ArrayList;

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * TODO Add javadoc
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class PEMHostKeyProvider extends AbstractKeyPairProvider {

    private String path;
    protected KeyPair keyPair;
    private final JcaPEMKeyConverter jcaHelper = new JcaPEMKeyConverter();

    public PEMHostKeyProvider(String path) {
        this.path = path;
        loadKeys();
        // load a host key, and ensure we don't create it.
        if (this.keyPair == null) {
            throw new RuntimeException("Unable to load hostkeys from " + path);
        }
    }

    protected KeyPair doReadKeyPair(InputStream is) throws Exception {
        try (PEMParser r = new PEMParser(new InputStreamReader(is))) {
            return jcaHelper.getKeyPair((PEMKeyPair) r.readObject());
        }
    }

    protected void doWriteKeyPair(KeyPair kp, OutputStream os) throws Exception {
        try (PEMWriter w = new PEMWriter(new OutputStreamWriter(os))) {
            w.writeObject(kp);
            w.flush();
        }
    }

    private KeyPair readKeyPair(File f) {
        try (InputStream is = new FileInputStream(f)) {
            return doReadKeyPair(is);
        } catch (Exception e) {
            log.info("Unable to read key {}: {}", path, e);
        }
        return null;
    }

    public synchronized Iterable<KeyPair> loadKeys() {
        ArrayList<KeyPair> keyPairList = new ArrayList<KeyPair>();

        if (keyPair == null) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                keyPair = readKeyPair(f);
            }
        }
        if (null != keyPair) {
            keyPairList.add(keyPair);
        }

        return keyPairList;
    }

}
