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

package com.baidu.openrasp.plugin.js.engine;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.tool.filemonitor.FileScanListener;
import com.baidu.openrasp.tool.filemonitor.FileScanMonitor;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
/**
 * Created by tyy on 4/5/17.
 * All rights reserved
 */

/**
 * PluginManager is a static class that encapsulates the details of the plugin system and exposes the init and check methods only to the outer layer.
 * <p>
 * PluginManager internal management plug-in system instance, monitoring detection script file changes
 * <p>
 * Must be initialized first
 */
public class JsPluginManager {

    private static final Logger LOGGER = Logger.getLogger(JsPluginManager.class.getPackage().getName() + ".log");
    private static Timer timer = null;
    private static Integer watchId = null;

    /**
     * Initialize the plugin engine
     *
     * @throws Exception
     */
    public synchronized static void init() throws Exception {
        JSContextFactory.init();
        updatePlugin();
        initFileWatcher();
    }

    public synchronized static void release() {
        HookHandler.enableHook.set(false);
        if (watchId != null) {
            FileScanMonitor.removeMonitor(watchId);
            watchId = null;
        }
        JSContextFactory.release();
    }

    /**
     * Initialization detection script file monitoring
     * <p>
     * Do not call, the detection script will not be automatically updated at runtime
     *
     * @throws Exception
     */
    public synchronized static void initFileWatcher() throws Exception {
        boolean oldValue = HookHandler.enableHook.getAndSet(false);
        if (watchId != null) {
            FileScanMonitor.removeMonitor(watchId);
            watchId = null;
        }
        watchId = FileScanMonitor.addMonitor(
                Config.getConfig().getScriptDirectory(),
                new FileScanListener() {
                    @Override
                    public void onFileCreate(File file) {
                        if (file.getName().endsWith(".js")) {
                            updatePluginAsync();
                        }
                    }

                    @Override
                    public void onFileChange(File file) {
                        if (file.getName().endsWith(".js")) {
                            updatePluginAsync();
                        }
                    }

                    @Override
                    public void onFileDelete(File file) {
                        if (file.getName().endsWith(".js")) {
                            updatePluginAsync();
                        }
                    }
                });
        HookHandler.enableHook.set(oldValue);
    }

    /**
     * Update plugin engine
     * <p>
     * Update when detecting script changes
     * <p>
     * Replace the old plugin engine after the new plugin engine is successfully initialized
     *
     * @throws Exception
     */
    private synchronized static void updatePlugin() throws Exception {
        / / Clear the algorithm.config configuration
        Config.getConfig().setAlgorithmConfig("{}");
        boolean oldValue = HookHandler.enableHook.getAndSet(false);
        File pluginDir = new File(Config.getConfig().getScriptDirectory());
        LOGGER.debug("checker directory: " + pluginDir.getAbsolutePath());
        if (!pluginDir.isDirectory()) {
            pluginDir.mkdir();
        }
        FileFilter filter = FileFilterUtils.and(FileFilterUtils.sizeFileFilter(10 * 1024 * 1024, false), FileFilterUtils.suffixFileFilter(".js"));
        File[] pluginFiles = pluginDir.listFiles(filter);
        List<CheckScript> scripts = new LinkedList<CheckScript>();
        if (pluginFiles != null) {
            for (File file : pluginFiles) {
                try {
                    scripts.add(new CheckScript(file));
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }
        }

        JSContextFactory.setCheckScriptList(scripts);

        HookHandler.enableHook.set(oldValue);
    }

    /**
     * Asynchronous update plugin engine
     * <p>
     * Avoids jitter generated when script files are updated in the file system
     * <p>
     * If there is jitter, increase the timer delay
     */
    private synchronized static void updatePluginAsync() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updatePlugin();
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        }, 500);
    }


}
