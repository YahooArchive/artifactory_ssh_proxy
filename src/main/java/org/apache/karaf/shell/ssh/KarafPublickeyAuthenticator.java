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
package org.apache.karaf.shell.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Comparator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.mina.util.Base64;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * we're not using karaf at all. However, we need this to do PublicKey Auth.
 * 
 * And don't monkey with formatting, code or anything else, this is a straight copy of:
 * http://svn.apache.org/repos/asf/karaf
 * /branches/karaf-2.2.x/shell/ssh/src/main/java/org/apache/karaf/shell/ssh/KarafPublickeyAuthenticator.java
 * 
 * Any changes have a // YAHOO - comment explaining the change. So far the only changes are this comment block and one
 * to remove private from the comparator.
 */

/**
 * A public key authenticator, which reads an OpenSSL2 <code>authorized_keys</code> file.
 */
public class KarafPublickeyAuthenticator implements PublickeyAuthenticator {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(KarafPublickeyAuthenticator.class);

    private String authorizedKeys;
    private boolean active;

    public static final class AuthorizedKey {

        private final String alias;
        private final String format;
        private final PublicKey publicKey;

        public AuthorizedKey(String alias, String format, PublicKey publicKey) {
            super();
            this.alias = alias;
            this.format = format;
            this.publicKey = publicKey;
        }

        public String getAlias() {
            return this.alias;
        }

        public String getFormat() {
            return this.format;
        }

        public PublicKey getPublicKey() {
            return this.publicKey;
        }

    }
    // YAHOO - remove private to allow this to be tested
    public static final class PublicKeyComparator implements Comparator<PublicKey> {

        @Override
        public int compare(PublicKey a, PublicKey b) {
            if (a instanceof DSAPublicKey) {
                if (b instanceof DSAPublicKey) {
                    DSAPublicKey da = (DSAPublicKey) a;
                    DSAPublicKey db = (DSAPublicKey) b;
                    int r = da.getParams().getG().compareTo(db.getParams().getG());
                    if (r != 0) {
                        return r;
                    }
                    r = da.getParams().getP().compareTo(db.getParams().getP());
                    if (r != 0) {
                        return r;
                    }
                    r = da.getParams().getQ().compareTo(db.getParams().getQ());
                    if (r != 0) {
                        return r;
                    }
                    return da.getY().compareTo(db.getY());
                } else {
                    return -1;
                }
            } else if (a instanceof RSAPublicKey) {
                if (b instanceof RSAPublicKey) {
                    RSAPublicKey da = (RSAPublicKey) a;
                    RSAPublicKey db = (RSAPublicKey) b;
                    int r = da.getPublicExponent().compareTo(db.getPublicExponent());
                    if (r != 0) {
                        return r;
                    }
                    return da.getModulus().compareTo(db.getModulus());
                } else {
                    return 1;
                }
            } else {
                throw new IllegalArgumentException("Only RSA and DAS keys are supported.");
            }
        }
    }

    private final class AuthorizedKeysProvider extends TimerTask {

        private Map<PublicKey, AuthorizedKey> keys;
        private Long lastModificationDate;
        private Boolean fileAvailable;

        @Override
        public void run() {
            try {
                File af = new File(KarafPublickeyAuthenticator.this.authorizedKeys);
                if (af.exists()) {
                    Long newModificationDate = Long.valueOf(af.lastModified());
                    if ((this.fileAvailable != null && !this.fileAvailable.booleanValue())
                                    || !newModificationDate.equals(this.lastModificationDate)) {
                        LOGGER.debug("Parsing authorized keys file {}...",
                                        KarafPublickeyAuthenticator.this.authorizedKeys);
                        this.fileAvailable = Boolean.TRUE;
                        this.lastModificationDate = newModificationDate;
                        Map<PublicKey, AuthorizedKey> newKeys =
                                        KarafPublickeyAuthenticator.parseAuthorizedKeys(new FileInputStream(af));
                        this.setKeys(newKeys);
                        LOGGER.debug("Successfully parsed {} keys from file {}", Integer.valueOf(newKeys.size()),
                                        KarafPublickeyAuthenticator.this.authorizedKeys);
                    }
                } else {
                    if (this.fileAvailable != null && this.fileAvailable.booleanValue()) {
                        LOGGER.debug("Authorized keys file {} disappeared, will recheck every minute",
                                        KarafPublickeyAuthenticator.this.authorizedKeys);
                    } else if (this.fileAvailable == null) {
                        LOGGER.debug("Authorized keys file {} does not exist, will recheck every minute",
                                        KarafPublickeyAuthenticator.this.authorizedKeys);
                    }
                    this.fileAvailable = Boolean.FALSE;
                    this.lastModificationDate = null;
                    this.setKeys(null);
                }
            } catch (Throwable e) {
                LOGGER.error("Error parsing authorized keys file {}", KarafPublickeyAuthenticator.this.authorizedKeys,
                                e);
                this.fileAvailable = Boolean.FALSE;
                this.lastModificationDate = null;
                this.setKeys(null);
            }
        }

        private synchronized void setKeys(Map<PublicKey, AuthorizedKey> keys) {
            this.keys = keys;
        }

        public synchronized AuthorizedKey getKey(PublicKey publicKey) {
            if (this.keys == null) {
                return null;
            }
            return this.keys.get(publicKey);
        }

    }

    private Timer parseAuthorizedKeysTimer;
    private AuthorizedKeysProvider authorizedKeysProvider;

    private static final int getInt(byte[] b, int pos) {
        return (((int) b[pos] & 0xff) << 24) + (((int) b[pos + 1] & 0xff) << 16) + (((int) b[pos + 2] & 0xff) << 8)
                        + ((int) b[pos + 3] & 0xff);
    }

    /**
     * Parse an <code>authorized_keys</code> file in OpenSSH style.
     * 
     * @param is the input stream to read.
     * @return a map of authorized public keys.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static final Map<PublicKey, AuthorizedKey> parseAuthorizedKeys(InputStream is) throws IOException,
                    NoSuchAlgorithmException, InvalidKeySpecException {
        LineNumberReader reader = null;
        try {
            Base64 decoder = new Base64();

            KeyFactory rsaKeyGen = KeyFactory.getInstance("RSA");
            KeyFactory dsaKeyGen = KeyFactory.getInstance("DSA");

            reader = new LineNumberReader(new InputStreamReader(is, "UTF-8"));

            Map<PublicKey, AuthorizedKey> ret = new TreeMap<PublicKey, AuthorizedKey>(new PublicKeyComparator());

            String line;

            while ((line = reader.readLine()) != null) {
                // YAHOO - separate string parsing in a single function;
                parseAuthorizedKeysLine(ret, decoder, rsaKeyGen, dsaKeyGen, line, reader.getLineNumber());
            }

            return ret;
        } finally {
            try {
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException ioe) {
            }
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }

    private static final Pattern PAT_WHITESPACE = Pattern.compile("[ \\t]+");

    static String[] splitLine(final String line) {
        // need to reduce to 3 fields.
        return PAT_WHITESPACE.split(line, 3);
    }

    public static boolean parseAuthorizedKeysLine(final Map<PublicKey, AuthorizedKey> ret, final Base64 decoder,
                    final KeyFactory rsaKeyGen, final KeyFactory dsaKeyGen, final String line, final int lineNumber)
                    throws InvalidKeySpecException, UnsupportedEncodingException {
        try {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                return false; // skip # lines might be comment
            }

            String[] tokens = splitLine(line);
            if (tokens.length != 3) {
                LOGGER.info("Authorized keys file line " + lineNumber + " does not contain 3 tokens. " + line);
                return false;
                // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                // " does not contain 3 tokens. " + line);
            }

            byte[] rawKey = decoder.decode(tokens[1].getBytes("UTF-8"));
            if (rawKey.length < 4) {
                LOGGER.info("Authorized keys file line " + lineNumber
                                + " contains a key with a format that does not match the first token. " + line);
                return false;
                // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                // " contains a key with a format that does not match the first token. " + line);
            }

            if (getInt(rawKey, 0) != 7 || !new String(rawKey, 4, 7, "UTF-8").equals(tokens[0])) {
                LOGGER.info("Authorized keys file line " + lineNumber
                                + " contains a key with a format that does not match the first token. " + line);
                return false;
                // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                // " contains a key with a format that does not match the first token. " + line);
            }

            PublicKey pk;
            if (tokens[0].equals("ssh-dss")) {
                int pos = 11;

                int n = getInt(rawKey, pos);
                pos += 4;
                BigInteger p = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                n = getInt(rawKey, pos);
                pos += 4;
                BigInteger q = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                n = getInt(rawKey, pos);
                pos += 4;
                BigInteger g = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                n = getInt(rawKey, pos);
                pos += 4;
                BigInteger y = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                if (pos != rawKey.length) {
                    LOGGER.info("Authorized keys file line " + lineNumber + " contains a DSA key with extra garbage.");
                    return false;
                    // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                    // " contains a DSA key with extra garbage.");
                }

                DSAPublicKeySpec ps = new DSAPublicKeySpec(y, p, q, g);
                pk = dsaKeyGen.generatePublic(ps);
            } else if (tokens[0].equals("ssh-rsa")) {
                int pos = 11;

                int n = getInt(rawKey, pos);
                pos += 4;
                BigInteger e = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                n = getInt(rawKey, pos);
                pos += 4;
                BigInteger modulus =
                                new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                pos += n;

                if (pos != rawKey.length) {
                    LOGGER.info("Authorized keys file line " + lineNumber + " contains a RSA key with extra garbage.");
                    return false;
                    // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                    // " contains a RSA key with extra garbage.");
                }

                RSAPublicKeySpec ps = new RSAPublicKeySpec(modulus, e);
                pk = rsaKeyGen.generatePublic(ps);
            } else {
                LOGGER.info("Authorized keys file line " + lineNumber + " does not start with ssh-dss or ssh-rsa.");
                return false;
                // throw new IOException("Authorized keys file line " + reader.getLineNumber() +
                // " does not start with ssh-dss or ssh-rsa.");
            }

            ret.put(pk, new AuthorizedKey(tokens[2], tokens[0], pk));

            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            // oops.
            LOGGER.info("Authorized keys file line " + lineNumber + " contains a key with invalid data. " + line, e);
            return false;
        }
    }

    @Override
    public boolean authenticate(String username, PublicKey publicKey, ServerSession session) {
        AuthorizedKey ak = this.authorizedKeysProvider.getKey(publicKey);
        if (ak == null) {
            LOGGER.error("Failed authenticate of user {} from {} with unknown public key.", username, session
                            .getIoSession().getRemoteAddress());
            return false;
        }
        LOGGER.debug("Successful authentication of user {} from {} with public key {}.", new Object[] {username,
                        session.getIoSession().getRemoteAddress(), ak.getAlias()});
        return true;
    }

    public void setAuthorizedKeys(String path) {
        this.authorizedKeys = path;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void startTimer() {
        if (this.active) {
            this.parseAuthorizedKeysTimer = new Timer();
            this.authorizedKeysProvider = new AuthorizedKeysProvider();
            this.parseAuthorizedKeysTimer.schedule(this.authorizedKeysProvider, 10, 60000L);
        }
    }

    public void stopTimer() {
        if (this.parseAuthorizedKeysTimer != null) {
            this.parseAuthorizedKeysTimer.cancel();
            this.parseAuthorizedKeysTimer = null;
        }
    }

    private static byte[] arraysCopyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }
}
