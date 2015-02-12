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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
import com.yahoo.sshd.server.jetty.JettyRunnableComponent;
import com.yahoo.sshd.server.settings.SshdProxySettings.ShellMode;
import com.yahoo.sshd.utils.RunnableComponent;

/**
 * This class is used for configuration of an {@link SshdProxySettings} implementation. In 0.1.X, a number of methods
 * were erroneously named getXXX, and then {@link SshdSettingsBuilder#build()} called a giant constructor. In 0.2.X,
 * findXXX builds the setting, and getXXX returns the final settings so {@link SshdSettingsBuilder#build()} can call a
 * constructor with an instance of this class.
 * 
 * @author areese
 * 
 */
public class SshdSettingsBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshdSettingsBuilder.class);

    /**
     * The name of this system, used in building some strings.
     */
    @Deprecated
    private static final String SYSTEM_NAME = "sshd_proxy";

    private static final String SYSTEM_NAME_DIR = File.separator + "sshd_proxy" + File.separator;

    /**
     * getRoot() is prepends to this.
     */
    private static final String DEFAULT_ROOT = "/opt/" + SYSTEM_NAME;

    /**
     * Default port to listen on, was 9000, but 2222 is a more standard port to run another sshd on.
     */
    private static final int DEFAULT_SSHD_PORT = 2222;

    /**
     * Default format for log files
     */
    private static final String LOG_FILE_DATE_FORMAT = "yyyy_MM_dd";

    /**
     * Default port for jetty to listen on. 8080 was picked because most apis run on 4080
     */
    public static final int DEFAULT_JETTY_PORT = -1;
    public static final String DEFAULT_JETTY_WEBAPP_DIR = DEFAULT_ROOT + "/webapps";

    private int sshdPort;
    private int httpPort;
    private String webappsDir;
    private String hostKeyPath;
    private String rootPath;

    private List<? extends DelegatingCommandFactory> commandFactories;

    private String artifactoryUrl;
    private String artifactoryUsername;
    private String artifactoryPassword;
    private Configuration configuration;
    private RunnableComponent[] externalComponents;
    private String artifactoryAuthorizationFilePath;
    private String requestLogPath;

    private Boolean developerMode;

    private ShellMode shellMode;

    protected String overriddenRoot;

    protected Map<String, String> envToAfPropertyMapping;

    static final List<String> DEFAULT_COMMAND_FACTORIES = new ArrayList<>(Arrays.asList(new String[] {//
                    DefaultScpCommandFactory.class.getCanonicalName(), //
                    }));

    protected SshdSettingsBuilder() {

    }

    public SshdSettingsBuilder(@Nonnull String[] args) throws SshdConfigurationException {
        String overriddenPath = null;
        // create the parser
        final CommandLineParser parser = new GnuParser();

        try {
            Options options = new Options();
            options.addOption("f", "config", true, "Path to properties file");
            options.addOption("r", "root", true, "root path under which things are stored");
            options.addOption("x", "xdeveloper", false, "Enable developer mode, disabling auth access control");

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            overriddenPath = line.getOptionValue('f');
            overriddenRoot = fixEmpty(line.getOptionValue('r'));
            developerMode = Boolean.valueOf(line.hasOption('x'));
        } catch (ParseException e) {
            throw new SshdConfigurationException(e);
        }

        try {
            configuration = findPropertiesConfiguration(overriddenPath);
        } catch (ConfigurationException e) {
            throw new SshdConfigurationException(e);
        }

        rootPath = findRoot(overriddenRoot);
        hostKeyPath = findHostKeyPath();
        sshdPort = findSshdPort();
        artifactoryUrl = findArtifactoryUrl();
        artifactoryUsername = findArtifactoryUsername();
        artifactoryPassword = findArtifactoryPassword();
        artifactoryAuthorizationFilePath = findArtifactoryAuthorizationFilePath();
        requestLogPath = findRequestLogPath();
        httpPort = findHttpPort();
        webappsDir = findWebappDir();
        shellMode = findShellMode();

        // do this last, so it can rely on everything before/
        externalComponents = createExternalComponents();
    }


    protected static String fixEmpty(String str) {
        if (null == str)
            return null;

        str = str.trim();
        if (str.isEmpty()) {
            return null;
        }

        return str;
    }

    /**
     * Generate the path for where request/access logs should be written to.
     * 
     * @return a path to write access logs to.
     */
    protected String findRequestLogPath() {
        final String defaultRequestLogFilePath = getLogsDir() + "access." + LOG_FILE_DATE_FORMAT + ".log";

        return getStringFromConfig("sshd.requestLogFilePath", defaultRequestLogFilePath, "got request log file path");
    }


    /**
     * Generate the path for where the authorization mapping file should be read from.
     * 
     * @return a path to write access logs to.
     */
    protected String findArtifactoryAuthorizationFilePath() {
        final String defaultArtifactoryAuthorizationFilePath = getAuthDir() + "auth.txt";

        return getStringFromConfig("sshd.artifactoryAuthorizationFilePath", defaultArtifactoryAuthorizationFilePath,
                        "got artifactory authorization file path");
    }

    /**
     * Return a new array of extra things that need to be started.
     * 
     * @return an array of {@link RunnableComponent} that also need to be started.
     */
    @SuppressWarnings("resource")
    protected RunnableComponent[] createExternalComponents() {
        if (-1 == httpPort || null == webappsDir) {
            return new RunnableComponent[] {};
        }

        return new RunnableComponent[] {new JettyRunnableComponent(httpPort, webappsDir),};
    }

    /**
     * Generate the URL to access artifactory at
     * 
     * @return the artifactory url to access.
     */
    protected String findArtifactoryUrl() {
        return getStringFromConfig("sshd.artifactoryUrl", "got artifactoryUrl");
    }

    /**
     * Generate the username to access artifactory as
     * 
     * @return the username to access artifactory as
     */
    protected String findArtifactoryUsername() {
        return getStringFromConfig("sshd.artifactoryUsername", "got artifactoryUsername");
    }

    /**
     * Generate the password to access artifactory with
     * 
     * @return the password to access artifactory with
     */
    protected String findArtifactoryPassword() {
        return getStringFromConfig("sshd.artifactoryPassword", "got artifactoryPassword");
    }

    /**
     * Generate the port to run jetty on.
     * 
     * @return the port to run jetty on, -1 if jetty is disabled
     */
    protected int findHttpPort() {
        // TODO: return -1 when jetty is disabled.
        final int port = configuration.getInt("sshd.jetty.port", DEFAULT_JETTY_PORT);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got jettyPort {}", Integer.valueOf(port));
        }

        return port;
    }


    /**
     * Generate the webapps dir for jetty
     * 
     * @returnthe webapps dir for jetty, null if jetty is disabled
     */
    protected String findWebappDir() {
        return getStringFromConfig("sshd.jetty.webapp.dir", DEFAULT_JETTY_WEBAPP_DIR, "got jettyWebappDir");
    }

    /**
     * Locate the hostkey path from the configuration
     * 
     * @return the hostKeyPath that should be set in the build for consumption by the {@link SshdSettingsInterface}
     *         implementation
     */
    protected String findHostKeyPath() {
        final String defaultHostKeyPath = getConfDir() + "ssh_host_dsa_key";

        return getStringFromConfig("sshd.hostKeyPath", defaultHostKeyPath, "got host key");
    }

    List<DelegatingCommandFactory> createCfInstances() {
        List<DelegatingCommandFactory> cfInstances = new ArrayList<>();
        cfInstances.add(new DefaultScpCommandFactory());
        return cfInstances;
    }

    /**
     * Find the root path A root path is where things are installed under.
     * 
     * @return root path to use
     */
    protected String findRoot(String overriddenRoot) {
        String root = System.getenv("ROOT");

        // if passed, forget the env, the commandline wins.
        overriddenRoot = fixEmpty(overriddenRoot);
        if (null != overriddenRoot) {
            root = overriddenRoot;
        }

        if (null == root) {
            root = DEFAULT_ROOT;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("default root set {} ", root);
            }
        }

        if (!root.endsWith("/")) {
            root += File.separatorChar;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got root {}", root);
        }

        return root;
    }

    protected Configuration createConfiguration(final String overriddenPath) {
        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("got overriddenPath {}", overriddenPath);
            }

            CompositeConfiguration compositeConfig = new CompositeConfiguration();
            // TODO: see how Systems and properties interact.
            compositeConfig.addConfiguration(new SystemConfiguration());
            compositeConfig.addConfiguration(findPropertiesConfiguration(overriddenPath));

            return compositeConfig;
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Configuration findPropertiesConfiguration(final String overriddenPath) throws ConfigurationException {
        String propertiesPath;
        if (null == overriddenPath || overriddenPath.isEmpty()) {
            propertiesPath = findPropertiesPath();
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

    protected String findPropertiesPath() {
        String propertiesPath;

        propertiesPath = getConfDir() + "/sshd_proxy.properties";

        propertiesPath = System.getProperty("sshd.propertiesFile", propertiesPath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got propertiesPath {}", propertiesPath);
        }

        return propertiesPath;
    }

    /**
     * Find the sshd port from the configuration
     * 
     * @return sshd port to be set in the builder.
     */
    protected int findSshdPort() {
        final int port = configuration.getInt("sshd.port", DEFAULT_SSHD_PORT);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got port {}", Integer.valueOf(port));
        }

        return port;
    }

    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    /**
     * Returns the System name to be used in constructing the paths, always prepended and appended by
     * {@link File#separator}
     * 
     * @return "/" + SYSTEM_NAME + "/"
     */
    protected String getSystemNameDir() {
        return SYSTEM_NAME_DIR;
    }


    /**
     * Returns the path to the configuration directory always appended by {@link File#separator}
     * 
     * @return the path to the configuration directory
     */
    public String getConfDir() {
        return getRoot() + "conf" + getSystemNameDir();
    }

    /**
     * Returns the path to the logs directory always appended by {@link File#separator}
     * 
     * @return the path to the logs directory
     */
    public String getLogsDir() {
        return getRoot() + "logs" + getSystemNameDir();
    }


    /**
     * Returns the path to the auth directory always appended by {@link File#separator}
     * 
     * @return the path to the auth directory
     */
    public String getAuthDir() {
        return getConfDir() + "auth" + File.separator;
    }

    /**
     * Return the root directory. Typically /opt, or /usr/local, everything lives in a structure below here:
     * 
     * ROOT comes from the env, but really should come from the command line. It was set in the env for a really bizarre
     * reason.
     * 
     * ROOT/conf/sshd_proxy/ ROOT/logs/sshd_proxy/
     * 
     * @return returns the root of where things live
     */
    @Deprecated
    public String getRoot() {
        if (null == rootPath) {
            rootPath = findRoot(overriddenRoot);
        }

        return rootPath;
    }

    protected int getSshdPort() {
        if (0 == sshdPort) {
            sshdPort = findSshdPort();
        }
        return sshdPort;
    }

    protected SshdSettingsBuilder setSshdPort(int sshdPort) {
        this.sshdPort = sshdPort;
        return this;
    }

    protected int getHttpPort() {
        if (0 == httpPort) {
            httpPort = findHttpPort();
        }
        return httpPort;
    }

    protected SshdSettingsBuilder setHttpPort(int httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    protected String getWebappsDir() {
        if (null == webappsDir || webappsDir.isEmpty()) {
            webappsDir = findWebappDir();
        }
        return webappsDir;
    }

    protected SshdSettingsBuilder setWebappsDir(String webappsDir) {
        this.webappsDir = webappsDir;
        return this;
    }

    protected String getHostKeyPath() {
        if (null == hostKeyPath) {
            hostKeyPath = findHostKeyPath();
        }
        return hostKeyPath;
    }

    protected SshdSettingsBuilder setHostKeyPath(String hostKeyPath) {
        this.hostKeyPath = hostKeyPath;
        return this;
    }

    protected SshdSettingsBuilder setRootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    protected List<? extends DelegatingCommandFactory> getCommandFactories() {
        if (null == commandFactories || commandFactories.isEmpty()) {
            commandFactories = createCfInstances();
        }
        return commandFactories;
    }

    protected SshdSettingsBuilder setCommandFactories(List<? extends DelegatingCommandFactory> commandFactories) {
        this.commandFactories = commandFactories;
        return this;
    }

    protected String getArtifactoryUrl() {
        if (null == artifactoryUrl) {
            artifactoryUrl = findArtifactoryUrl();
        }
        return artifactoryUrl;
    }

    protected SshdSettingsBuilder setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
        return this;
    }

    protected String getArtifactoryUsername() {
        if (null == artifactoryUsername) {
            artifactoryUsername = findArtifactoryUsername();
        }
        return artifactoryUsername;
    }

    protected SshdSettingsBuilder setArtifactoryUsername(String artifactoryUsername) {
        this.artifactoryUsername = artifactoryUsername;
        return this;
    }

    protected String getArtifactoryPassword() {
        if (null == artifactoryPassword) {
            artifactoryPassword = findArtifactoryPassword();
        }
        return artifactoryPassword;
    }

    protected SshdSettingsBuilder setArtifactoryPassword(String artifactoryPassword) {
        this.artifactoryPassword = artifactoryPassword;
        return this;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected SshdSettingsBuilder setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    protected RunnableComponent[] getExternalComponents() {
        if (null == externalComponents) {
            externalComponents = createExternalComponents();
        }
        return externalComponents;
    }

    protected SshdSettingsBuilder setExternalComponents(RunnableComponent[] externalComponents) {
        this.externalComponents = externalComponents;
        return this;
    }

    protected String getArtifactoryAuthorizationFilePath() {
        if (null == artifactoryAuthorizationFilePath) {
            artifactoryAuthorizationFilePath = findArtifactoryAuthorizationFilePath();
        }
        return artifactoryAuthorizationFilePath;
    }

    protected SshdSettingsBuilder setArtifactoryAuthorizationFilePath(String artifactoryAuthorizationFilePath) {
        this.artifactoryAuthorizationFilePath = artifactoryAuthorizationFilePath;
        return this;
    }

    protected String getRequestLogPath() {
        if (null == requestLogPath) {
            requestLogPath = findRequestLogPath();
        }
        return requestLogPath;
    }

    protected SshdSettingsBuilder setRequestLogPath(String requestLogPath) {
        this.requestLogPath = requestLogPath;
        return this;
    }

    protected static String getDefaultRoot() {
        return DEFAULT_ROOT;
    }

    protected static int getDefaultSshdPort() {
        return DEFAULT_SSHD_PORT;
    }

    protected static String getLogFileDateFormat() {
        return LOG_FILE_DATE_FORMAT;
    }

    protected static int getDefaultJettyPort() {
        return DEFAULT_JETTY_PORT;
    }

    protected static String getDefaultJettyWebappDir() {
        return DEFAULT_JETTY_WEBAPP_DIR;
    }

    public static List<String> getDefaultCommandFactories() {
        return DEFAULT_COMMAND_FACTORIES;
    }

    public SshdSettingsInterface build() throws SshdConfigurationException {
        return new SshdProxySettings(this);
    }

    public String getStringFromConfig(String config, String message) {
        return getStringFromConfig(config, null, message);
    }

    public String getStringFromConfig(String config, String defaultValue, String message) {
        final String s = configuration.getString(config, defaultValue);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", message, s);
        }

        if (null != s) {
            return s.trim();
        }
        return s;
    }


    public int getIntFromConfig(String config, int defaultValue, String message) {
        final int intValue = configuration.getInt(config, defaultValue);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", message, Integer.valueOf(intValue));
        }

        return intValue;
    }

    public boolean getDevelopmentMode() {
        return (null == developerMode) ? false : developerMode.booleanValue();
    }


    /**
     * Generate the path for where request/access logs should be written to.
     * 
     * @return a path to write access logs to.
     */
    protected ShellMode findShellMode() {
        ShellMode defaultMode = ShellMode.MESSAGE;
        String modeString = getStringFromConfig("sshd.shellMode", defaultMode.name(), "got ShellMode");
        return ShellMode.valueOf(modeString);
    }


    public SshdSettingsBuilder setSetShellMode(ShellMode shellMode) {
        this.shellMode = shellMode;
        return this;
    }

    public ShellMode getShellMode() {
        if (null == shellMode) {
            shellMode = findShellMode();
        }
        return shellMode;
    }

    public Map<String, String> getEnvToAfPropertyMapping() {
        if (null == envToAfPropertyMapping) {
            envToAfPropertyMapping = findEnvToAfPropertyMapping();
        }
        return envToAfPropertyMapping;
    }

    // the iterator adds it's own period...
    static final String ENV_MAPPING_PREFIX = "sshd.envMapping";

    protected Map<String, String> findEnvToAfPropertyMapping() {
        HashMap<String, String> mapping = new HashMap<>();
        Iterator<String> keys = configuration.getKeys(ENV_MAPPING_PREFIX);
        if (null == keys) {
            return mapping;
        }

        // add the stupid dot on.
        final int offset = ENV_MAPPING_PREFIX.length() + 1;

        while (keys.hasNext()) {
            String nextKey = keys.next();
            if (nextKey.length() <= offset) {
                continue;
            }

            String envName = nextKey.substring(offset);
            String value = configuration.getString(nextKey);

            mapping.put(envName, value);
        }

        return mapping;
    }
}
