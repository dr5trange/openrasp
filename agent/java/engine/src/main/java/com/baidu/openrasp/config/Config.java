/*
 * Copyright 2017-2018 Baidu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.openrasp.config;

import com.baidu.openrasp.exception.ConfigLoadException;
import com.baidu.openrasp.tool.FileUtil;
import com.baidu.openrasp.tool.filemonitor.FileScanListener;
import com.baidu.openrasp.tool.filemonitor.FileScanMonitor;
import com.fuxi.javaagent.contentobjects.jnotify.JNotifyException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


/**
 * Created by tyy on 3/27/17.
 * Project configuration class, load configuration by parsing conf/rasp.property file
 * If the configuration file is not found, the default value is used.
 */
public class Config extends FileScanListener {

    public enum Item {
        PLUGIN_TIMEOUT_MILLIS("plugin.timeout.millis", "100"),
        HOOKS_IGNORE("hooks.ignore", ""),
        BLOCK_URL("block.url", "https://rasp.baidu.com/blocked"),
        READ_FILE_EXTENSION_REGEX("readfile.extension.regex", "^(gz|7z|xz|tar|rar|zip|sql|db)$"),
        INJECT_URL_PREFIX("inject.urlprefix", ""),
        REQUEST_PARAM_ENCODING("request.param_encoding", ""),
        BODY_MAX_BYTES("body.maxbytes", "4096"),
        LOG_MAX_STACK("log.maxstack", "20"),
        REFLECTION_MAX_STACK("plugin.maxstack", "100"),
        SECURITY_ENFORCE_POLICY("security.enforce_policy", "false"),
        OGNL_EXPRESSION_MIN_LENGTH("ognl.expression.minlength", "30"),
        SQL_SLOW_QUERY_MIN_ROWS("sql.slowquery.min_rows", "500"),
        BLOCK_STATUS_CODE("block.status_code", "302"),
        DEBUG("debug.level", "0"),
        ALGORITHM_CONFIG("algorithm.config", "{}", false);


        Item(String key, String defaultValue) {
            this(key, defaultValue, true);
        }

        Item(String key, String defaultValue, boolean isProperties) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.isProperties = isProperties;
        }

        String key;
        String defaultValue;
        boolean isProperties;

        @Override
        public String toString() {
            return key;
        }
    }

    private static final String CONFIG_DIR_NAME = "conf";
    private static final String CONFIG_FILE_NAME = "rasp.properties";
    public static final int REFLECTION_STACK_START_INDEX = 0;
    public static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    public static String baseDirectory;
    private static Integer watchId;

    private String configFileDir;
    private int pluginMaxStack;
    private long pluginTimeout;
    private int bodyMaxBytes;
    private int sqlSlowQueryMinCount;
    private String[] ignoreHooks;
    private boolean enforcePolicy;
    private String[] reflectionMonitorMethod;
    private int logMaxStackSize;
    private String readFileExtensionRegex;
    private String blockUrl;
    private String injectUrlPrefix;
    private String requestParamEncoding;
    private int ognlMinLength;
    private int blockStatusCode;
    private int debugLevel;
    private JsonObject algorithmConfig;

    static {
        baseDirectory = FileUtil.getBaseDir();
        CustomResponseHtml.load(baseDirectory);
        try {
            FileScanMonitor.addMonitor(
                    baseDirectory, ConfigHolder.instance);
        } catch (JNotifyException e) {
            throw new ConfigLoadException("add listener on " + baseDirectory + " failed because:" + e.getMessage());
        }
        LOGGER.info("baseDirectory: " + baseDirectory);
    }

    /**
     * constructor, initialize the global configuration
     */
    private Config() {
        this.configFileDir = baseDirectory + File.separator + CONFIG_DIR_NAME;
        String configFilePath = this.configFileDir + File.separator + CONFIG_FILE_NAME;
        try {
            loadConfigFromFile(new File(configFilePath), true);
            addConfigFileMonitor();
        } catch (FileNotFoundException e) {
            handleException("Could not find rasp.properties, using default settings: " + e.getMessage(), e);
        } catch (JNotifyException e) {
            handleException("add listener on " + configFileDir + " failed because:" + e.getMessage(), e);
        } catch (IOException e) {
            handleException("cannot load properties file: " + e.getMessage(), e);
        }
    }

    private synchronized void loadConfigFromFile(File file, boolean isInit) throws IOException {
        Properties properties = new Properties();
        try {
            if (file.exists()) {
                FileInputStream input = new FileInputStream(file);
                properties.load(input);
                input.close();
            }
        } finally {
            // There is a parsing problem with default values
            for (Item item : Item.values()) {
                if (item.isProperties) {
                    setConfigFromProperties(item, properties, isInit);
                }
            }
        }
    }

    private void reloadConfig(File file) {
        if (file.getName().equals(CONFIG_FILE_NAME)) {
            try {
                loadConfigFromFile(file, false);
            } catch (IOException e) {
                LOGGER.warn("update rasp.properties failed because: " + e.getMessage());
            }
        }
    }

    private void addConfigFileMonitor() throws JNotifyException {
        if (watchId != null) {
            FileScanMonitor.removeMonitor(watchId);
        }
        watchId = FileScanMonitor.addMonitor(configFileDir, new FileScanListener() {
            @Override
            public void onFileCreate(File file) {
                reloadConfig(file);
            }

            @Override
            public void onFileChange(File file) {
                reloadConfig(file);
            }

            @Override
            public void onFileDelete(File file) {
                reloadConfig(file);
            }
        });
    }

    private void setConfigFromProperties(Item item, Properties properties, boolean isInit) {
        String key = item.key;
        String value = properties.getProperty(item.key, item.defaultValue);
        try {
            setConfig(key, value, isInit);
        } catch (Exception e) {
            // There is a parsing problem with default values
            value = item.defaultValue;
            setConfig(key, item.defaultValue, false);
            LOGGER.warn("set config " + item.key + " failed, use default value : " + value);
        }
    }

    private void handleException(String message, Exception e) {
        LOGGER.warn(message);
        System.out.println(message);
    }

    private static class ConfigHolder {
        static Config instance = new Config();
    }

    /**
     * Get configuration singleton
     *
     * @return Config singleton object
     */
    public static Config getConfig() {
        return ConfigHolder.instance;
    }

    /**
     * Get the directory where the current jar package is located
     *
     * @return current jar package directory
     */
    public String getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Get the directory where the js script is located
     *
     * @return js script directory
     */
    public String getScriptDirectory() {
        return baseDirectory + "/plugins";
    }

    /**
     * Get a custom js script to insert html pages
     *
     * @return js script content
     */
    public String getCustomResponseScript() {
        return CustomResponseHtml.getInstance() != null ? CustomResponseHtml.getInstance().getContent() : null;
    }

    @Override
    public void onDirectoryCreate(File file) {
        reloadConfigDir(file);
    }

    @Override
    public void onDirectoryDelete(File file) {
        reloadConfigDir(file);
    }

    @Override
    public void onFileCreate(File file) {
        // ignore
    }

    @Override
    public void onFileChange(File file) {
        // ignore
    }

    @Override
    public void onFileDelete(File file) {
        // ignore
    }

    private void reloadConfigDir(File directory) {
        try {
            if (directory.getName().equals(CustomResponseHtml.CUSTOM_RESPONSE_BASE_DIR)) {
                CustomResponseHtml.load(baseDirectory);
            } else if (directory.getName().equals(CONFIG_DIR_NAME)) {
                reloadConfig(new File(configFileDir + File.separator + CONFIG_FILE_NAME));
            }
        } catch (Exception e) {
            LOGGER.warn("update " + directory.getAbsolutePath() + " failed because: " + e.getMessage());
        }
    }

    //-------------------- Configuration items that can be modified by the plugin -------------------- --------------

    /**
     * Get Js engine execution timeout
     *
     * @return timeout
     */
    public synchronized long getPluginTimeout() {
        return pluginTimeout;
    }

    /**
     * Configure Js engine execution timeout
     *
     * @param pluginTimeout timeout
     */
    public synchronized void setPluginTimeout(String pluginTimeout) {
        this.pluginTimeout = Long.parseLong(pluginTimeout);
        if (this.pluginTimeout < 0) {
            this.pluginTimeout = 0;
        }
    }

    /**
     * Set the page path prefix to insert custom html
     *
     * @return page path prefix
     */
    public synchronized String getInjectUrlPrefix() {
        return injectUrlPrefix;
    }

    /**
     * Get the page path prefix that needs to insert custom html
     *
     * @param injectUrlPrefix page path prefix
     */
    public synchronized void setInjectUrlPrefix(String injectUrlPrefix) {
        StringBuilder injectPrefix = new StringBuilder(injectUrlPrefix);
        while (injectPrefix.length() > 0 && injectPrefix.charAt(injectPrefix.length() - 1) == '/') {
            injectPrefix.deleteCharAt(injectPrefix.length() - 1);
        }
        this.injectUrlPrefix = injectPrefix.toString();
    }

    /**
     * Maximum save length when saving HTTP request body
     *
     * @return maximum length
     */
    public synchronized int getBodyMaxBytes() {
        return bodyMaxBytes;
    }

    /**
     * Configure the maximum save length when saving the HTTP request body
     *
     * @param bodyMaxBytes
     */
    public synchronized void setBodyMaxBytes(String bodyMaxBytes) {
        this.bodyMaxBytes = Integer.parseInt(bodyMaxBytes);
        if (this.bodyMaxBytes < 0) {
            this.bodyMaxBytes = 0;
        }
    }

    public synchronized int getSqlSlowQueryMinCount() {
        return sqlSlowQueryMinCount;
    }

    public synchronized void setSqlSlowQueryMinCount(String sqlSlowQueryMinCount) {
        this.sqlSlowQueryMinCount = Integer.parseInt(sqlSlowQueryMinCount);
        if (this.sqlSlowQueryMinCount < 0) {
            this.sqlSlowQueryMinCount = 0;
        }
    }

    /**
     * Hook points to ignore
     *
     * @return list of hit points to ignore
     */
    public synchronized String[] getIgnoreHooks() {
        return this.ignoreHooks;
    }

    /**
     * Configure hook points to ignore
     *
     * @param ignoreHooks
     */
    public synchronized void setIgnoreHooks(String ignoreHooks) {
        this.ignoreHooks = ignoreHooks.replace(" ", "").split(",");
    }

    /**
     * The maximum depth of the information that the reflection hook passes to the plugin stack
     *
     * @return stack information maximum depth
     */
    public synchronized int getPluginMaxStack() {
        return pluginMaxStack;
    }

    /**
     * Set the maximum depth of the reflection hook information passed to the plugin stack information
     *
     * @param pluginMaxStack stack information maximum depth
     */
    public synchronized void setPluginMaxStack(String pluginMaxStack) {
        this.pluginMaxStack = Integer.parseInt(pluginMaxStack);
        if (this.pluginMaxStack < 0) {
            this.pluginMaxStack = 0;
        }
    }

    /**
     * Get the method of reflection monitoring
     *
     * @return reflection method that needs to be monitored
     */
    public synchronized String[] getReflectionMonitorMethod() {
        return reflectionMonitorMethod;
    }

    /**
     * Set the method of reflection monitoring
     *
     * @param reflectionMonitorMethod Method of monitoring
     */
    public synchronized void setReflectionMonitorMethod(String reflectionMonitorMethod) {
        this.reflectionMonitorMethod = reflectionMonitorMethod.replace(" ", "").split(",");
    }

    /**
     * Get the url that intercepts the custom page
     *
     * @return intercept page url
     */
    public synchronized String getBlockUrl() {
        return blockUrl;
    }

    /**
     * Set the intercept page url
     *
     * @param blockUrl intercept page url
     */
    public synchronized void setBlockUrl(String blockUrl) {
        this.blockUrl = StringUtils.isEmpty(blockUrl) ? Item.BLOCK_URL.defaultValue : blockUrl;
    }

    /**
     * Get the maximum output stack depth of the alarm log
     *
     * @return
     */
    public synchronized int getLogMaxStackSize() {
        return logMaxStackSize;
    }

    /**
     * Configure the alarm log maximum output stack depth
     *
     * @param logMaxStackSize
     */
    public synchronized void setLogMaxStackSize(String logMaxStackSize) {
        this.logMaxStackSize = Integer.parseInt(logMaxStackSize);
        if (this.logMaxStackSize < 0) {
            this.logMaxStackSize = 0;
        }
    }

    /**
     * Get the minimum length of the ognl expression that is allowed to be passed to the plugin
     *
     * @return ognl expression minimum length
     */
    public synchronized int getOgnlMinLength() {
        return ognlMinLength;
    }

    /**
     * Configure the minimum length of the ognl expression that is allowed to be passed to the plugin
     *
     * @param ognlMinLength ognl expression minimum length
     */
    public synchronized void setOgnlMinLength(String ognlMinLength) {
        this.ognlMinLength = Integer.parseInt(ognlMinLength);
        if (this.ognlMinLength < 0) {
            this.ognlMinLength = 0;
        }
    }

    /**
     * Whether to open mandatory security regulations
     * If the detection is safe, the server will be disabled.
     * If you turn off the log warning when there is a security risk
     *
     * @return true on, false off
     */
    public synchronized boolean getEnforcePolicy() {
        return enforcePolicy;
    }

    /**
     * Configure whether to enable mandatory security specifications
     *
     * @return true on, false off
     */
    public synchronized void setEnforcePolicy(String enforcePolicy) {
        this.enforcePolicy = Boolean.parseBoolean(enforcePolicy);
    }

    /**
     * Get the extension regular expression that the read file needs to detect
     *
     * @return
     */
    public synchronized String getReadFileExtensionRegex() {
        return readFileExtensionRegex;
    }

    /**
     * Set the extension regular expression that the read file needs to detect
     *
     * @param readFileExtensionRegex
     */
    public synchronized void setReadFileExtensionRegex(String readFileExtensionRegex) {
        this.readFileExtensionRegex = readFileExtensionRegex;
    }

    /**
     * Get the interception status code
     *
     * @return status code
     */
    public synchronized int getBlockStatusCode() {
        return blockStatusCode;
    }

    /**
     * Set the interception status code
     *
     * @param blockStatusCode status code
     */
    public synchronized void setBlockStatusCode(String blockStatusCode) {
        this.blockStatusCode = Integer.parseInt(blockStatusCode);
        if (this.blockStatusCode < 100 || this.blockStatusCode > 999) {
            this.blockStatusCode = 302;
        }
    }

    /**
     * Get the debugLevel level
     * 0 is off, not 0 is on
     *
     * @return debugLevel level
     */
    public synchronized int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Whether to turn on debugging
     *
     * @return true means open
     */
    public synchronized boolean isDebugEnabled() {
        return debugLevel > 0;
    }

    /**
     * Set the debugLevel level
     *
     * @param debugLevel debugLevel level 
     */
    public synchronized void setDebugLevel(String debugLevel) {
        this.debugLevel = Integer.parseInt(debugLevel);
        if (this.debugLevel < 0) {
            this.debugLevel = 0;
        } else if (this.debugLevel > 0) {
            String debugEnableMessage = "[OpenRASP] Debug output enabled, debug_level=" + debugLevel;
            System.out.println(debugEnableMessage);
            LOGGER.info(debugEnableMessage);
        }
    }

    /**
     * Get detection algorithm configuration
     *
     * @return configured json object
     */
    public synchronized JsonObject getAlgorithmConfig() {
        return algorithmConfig;
    }

    /**
     * Set detection algorithm configuration
     *
     * @param json configuration content
     */
    public synchronized void setAlgorithmConfig(String json) {
        this.algorithmConfig = new JsonParser().parse(json).getAsJsonObject();
    }

    /**
     * Get request parameter encoding
     *
     * @return request parameter encoding
     */
    public synchronized String getRequestParamEncoding() {
        return requestParamEncoding;
    }

    /**
     * Set request parameter encoding
     * When the configuration is not empty, it will allow the hook point (such as request hook point) to get the parameter before the user according to the set encoding.
     * (Note: If the encoding is set, all request parameters will be decoded according to this encoding. If the user has multiple encodings for the parameters, it is recommended not to add this configuration)
     * When the configuration is empty, only the user can obtain the parameters after the parameters are obtained, thus preventing the garbled problem.
     *
     * @param requestParamEncoding request parameter encoding
     */
    public synchronized void setRequestParamEncoding(String requestParamEncoding) {
        this.requestParamEncoding = requestParamEncoding;
    }

    //-------------------------- Unified configuration processing ------------------ ------------------

    /**
     * Unified configuration interface, change the configuration entry through js
     *
     * @param key configuration name
     * @param value configuration value
     * @return is configured successfully
     */
    public boolean setConfig(String key, String value, boolean isInit) {
        try {
            boolean isHit = true;
            if (Item.BLOCK_URL.key.equals(key)) {
                setBlockUrl(value);
            } else if (Item.BODY_MAX_BYTES.key.equals(key)) {
                setBodyMaxBytes(value);
            } else if (Item.HOOKS_IGNORE.key.equals(key)) {
                setIgnoreHooks(value);
            } else if (Item.INJECT_URL_PREFIX.key.equals(key)) {
                setInjectUrlPrefix(value);
            } else if (Item.LOG_MAX_STACK.key.equals(key)) {
                setLogMaxStackSize(value);
            } else if (Item.OGNL_EXPRESSION_MIN_LENGTH.key.equals(key)) {
                setOgnlMinLength(value);
            } else if (Item.PLUGIN_TIMEOUT_MILLIS.key.equals(key)) {
                setPluginTimeout(value);
            } else if (Item.READ_FILE_EXTENSION_REGEX.key.equals(key)) {
                setReadFileExtensionRegex(value);
            } else if (Item.REFLECTION_MAX_STACK.key.equals(key)) {
                setPluginMaxStack(value);
            } else if (Item.SECURITY_ENFORCE_POLICY.key.equals((key))) {
                setEnforcePolicy(value);
            } else if (Item.SQL_SLOW_QUERY_MIN_ROWS.key.equals(key)) {
                setSqlSlowQueryMinCount(value);
            } else if (Item.BLOCK_STATUS_CODE.key.equals(key)) {
                setBlockStatusCode(value);
            } else if (Item.DEBUG.key.equals(key)) {
                setDebugLevel(value);
            } else if (Item.ALGORITHM_CONFIG.key.equals(key)) {
                setAlgorithmConfig(value);
            } else if (Item.REQUEST_PARAM_ENCODING.key.equals(key)) {
                setRequestParamEncoding(value);
            } else {
                isHit = false;
            }
            if (isHit) {
                if (isInit) {
                    LOGGER.info(key + ": " + value);
                } else {
                    LOGGER.info("configuration item \"" + key + "\" changed to \"" + value + "\"");
                }
            } else {
                LOGGER.info("configuration item \"" + key + "\" doesn't exist");
                return false;
            }
        } catch (Exception e) {
            if (isInit) {
                / / Initial configuration process, if the error needs to continue to use the default value to execute
                throw new ConfigLoadException(e);
            }
            LOGGER.info("configuration item \"" + key + "\" failed to change to \"" + value + "\"" + " because:" + e.getMessage());
            return false;
        }
        return true;
    }

}
