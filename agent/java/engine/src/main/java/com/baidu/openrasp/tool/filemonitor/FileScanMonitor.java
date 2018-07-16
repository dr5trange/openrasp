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

package com.baidu.openrasp.tool.filemonitor;

import com.baidu.openrasp.config.Config;
import com.fuxi.javaagent.contentobjects.jnotify.JNotify;
import com.fuxi.javaagent.contentobjects.jnotify.JNotifyException;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

/**
 * Created by tyy on 4/17/17.
 * Monitor of file events, monitoring unit is folder
 */
public class FileScanMonitor {

    static {
        JnotifyWatcher watcher=new JnotifyWatcher();
        JNotify.init(Config.baseDirectory,watcher);
    }

    /**
     * Increase monitor
     *
     * @param path Listening folder path
     * @param listener event callback interface
     * @return listener id
     * @throws JNotifyException {@link JNotifyException}
     */
    public static int addMonitor(String path, FileAlterationListener listener) throws JNotifyException {
        File file = new File(path);
        FileAlterationObserver observer = new FileAlterationObserver(file);
        observer.checkAndNotify();
        observer.addListener(listener);
        int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
                | JNotify.FILE_MODIFIED;
        return JNotify.addWatch(path, mask, false, new FileEventListener(observer));
    }

    /**
     * Remove a folder to listen to
     *
     * @param watchId Increases the listener id returned when the folder listens
     */
    public static void removeMonitor(int watchId) {
        try {
            JNotify.removeWatch(watchId);
        } catch (JNotifyException e) {
            e.printStackTrace();
        }
    }


}
