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
package com.yahoo.sshd.server.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.sshd.server.command.DefaultScpCommandFactory;
import com.yahoo.sshd.server.command.DelegatingCommandFactory;
import com.yahoo.sshd.utils.RunnableComponent;

public class SshdSettingsBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshdSettingsBuilder.class);

    private static final String SYSTEM_NAME = "sshd_proxy";
    private static final String DEFAULT_ROOT = "/opt/" + SYSTEM_NAME;
    /**
     * getRoot() is prepended to this.
     */
    private static final int DEFAULT_SSHD_PORT = 9000;
    private static final String LOG_FILE_DATE_FORMAT = "yyyy_MM_dd";

    protected int port;
    protected String hostKeyPath;
    protected List<String> commandFactories = new ArrayList<>();

    protected String artifactoryUrl;
    protected String artifactoryUsername;
    protected String artifactoryPassword;
    protected Configuration configuration;
    protected RunnableComponent[] externalComponents;
    protected String artifactoryAuthorizationFilePath;
    protected String requestLogPath;

    static final List<String> DEFAULT_COMMAND_FACTORIES = new ArrayList<>(Arrays.asList(new String[] {//
                    DefaultScpCommandFactory.class.getCanonicalName(), //
                    }));

    public SshdSettingsBuilder(String[] args) throws SshdConfigurationException {
        // create the parser
        final CommandLineParser parser = new GnuParser();
        String overriddenPath = null;

        try {
            Options options = new Options();
            options.addOption("f", "config", true, "Path to properties file");

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            overriddenPath = line.getOptionValue('f');
        } catch (ParseException e) {
            throw new SshdConfigurationException(e);
        }

        try {
            configuration = getPropertiesConfiguration(overriddenPath);
        } catch (ConfigurationException e) {
            throw new SshdConfigurationException(e);
        }

        hostKeyPath = getHostKeyPath();
        port = getSshdPort();
        artifactoryUrl = getArtifactoryUrl();
        artifactoryUsername = getArtifactoryUsername();
        artifactoryPassword = getArtifactoryPassword();
        externalComponents = getExternalComponents();
        artifactoryAuthorizationFilePath = getArtifactoryAuthorizationFilePath();
        requestLogPath = getRequestLogPath();
    }

    protected String getRequestLogPath() {
        final String defaultRequestLogFilePath =
                        getRoot() + "/logs/" + getSystemName() + "/access." + LOG_FILE_DATE_FORMAT + ".log";

        final String logFilePath = configuration.getString("sshd.requestLogFilePath", defaultRequestLogFilePath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got request log file path {}", logFilePath);
        }
        return logFilePath;
    }

    protected String getArtifactoryAuthorizationFilePath() {
        final String defaultArtifactoryAuthorizationFilePath = getRoot() + "conf/" + getSystemName() + "/auth/auth.txt";

        final String s =
                        configuration.getString("sshd.artifactoryAuthorizationFilePath",
                                        defaultArtifactoryAuthorizationFilePath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got artifactory authorization file path {}", s);
        }
        return s;
    }

    protected RunnableComponent[] getExternalComponents() {
        return new RunnableComponent[] {};
    }

    protected String getArtifactoryUrl() {
        final String s = configuration.getString("sshd.artifactoryUrl");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got artifactoryUrl {}", s);
        }

        return s;
    }

    protected String getArtifactoryUsername() {
        final String s = configuration.getString("sshd.artifactoryUsername");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got artifactoryUrl {}", s);
        }

        return s;

    }

    protected String getArtifactoryPassword() {
        final String s = configuration.getString("sshd.artifactoryPassword");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got artifactoryPassword {}", s);
        }

        return s;
    }

    protected String getHostKeyPath() {
        final String defaultHostKeyPath = getRoot() + "/conf/" + getSystemName() + "/ssh_host_dsa_key";

        final String s = configuration.getString("sshd.hostKeyPath", defaultHostKeyPath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got host key {}", s);
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    List<DelegatingCommandFactory> createCfInstances() {
        if (commandFactories.size() == 0) {
            commandFactories = DEFAULT_COMMAND_FACTORIES;
        }

        List<DelegatingCommandFactory> cfInstances = new ArrayList<>(commandFactories.size());

        for (String cfClass : commandFactories) {
            try {
                Class<DelegatingCommandFactory> classInstance;

                try {
                    classInstance = (Class<DelegatingCommandFactory>) Class.forName(cfClass);
                } catch (ClassNotFoundException e) {
                    // TODO This hack exists to allow for some testing on the command line, and is probably useless at this point.
                    String newClass = "com.yahoo.sshd.server.command." + cfClass.trim();
                    LOGGER.error("failed to load class " + cfClass + " trying  " + newClass, e);
                    classInstance = (Class<DelegatingCommandFactory>) Class.forName(newClass);
                }
                DelegatingCommandFactory newCommandFactory = classInstance.newInstance();
                cfInstances.add(newCommandFactory);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.error(cfClass + " couldn't be found ", e);
            }
        }

        return cfInstances;
    }

    String getRoot() {
        String root = System.getenv("ROOT");

        if (null == root) {
            root = DEFAULT_ROOT;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("default root set {} ", root);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got root {}", root);
        }

        return root;
    }

    protected Configuration getConfiguration(final String overriddenPath) {
        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("got overriddenPath {}", overriddenPath);
            }

            CompositeConfiguration compositeConfig = new CompositeConfiguration();
            // TODO: see how Systems and properties interact.
            compositeConfig.addConfiguration(new SystemConfiguration());
            compositeConfig.addConfiguration(getPropertiesConfiguration(overriddenPath));

            return compositeConfig;
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Configuration getPropertiesConfiguration(final String overriddenPath) throws ConfigurationException {

        String propertiesPath;
        if (null == overriddenPath || overriddenPath.isEmpty()) {
            propertiesPath = getPropertiesPath();
        } else {
            // -f on command line overrides -D
            propertiesPath = overriddenPath;
        }

        if (!propertiesPath.startsWith("file:")) {
            if (!propertiesPath.startsWith("/")) {
                File f = new File(propertiesPath);
                propertiesPath = f.getAbsolutePath();
                propertiesPath.replace('\\', '/');
            }
            propertiesPath = "file:" + propertiesPath;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got propertiesPath {}", propertiesPath);
        }

        return new PropertiesConfiguration(propertiesPath);
    }

    protected String getPropertiesPath() {
        String propertiesPath;

        propertiesPath = getRoot() + "/conf/" + getSystemName() + "/sshd_proxy.properties";

        propertiesPath = System.getProperty("sshd.propertiesFile", propertiesPath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got propertiesPath {}", propertiesPath);
        }

        return propertiesPath;
    }

    protected int getSshdPort() {
        final int port = configuration.getInt("sshd.port", DEFAULT_SSHD_PORT);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got port {}", Integer.valueOf(port));
        }

        return port;
    }

    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    public SshdSettingsInterface build() throws SshdConfigurationException {
        return new SshdProxySettings(port, hostKeyPath, createCfInstances(), artifactoryUrl, artifactoryUsername,
                        artifactoryPassword, externalComponents, artifactoryAuthorizationFilePath, requestLogPath);
    }
}
