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


import com.fuxi.javaagent.contentobjects.jnotify.JNotifyListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

/**
 * Created by tyy on 6/7/17.
 * Listener that can be used to listen to a folder event
 * Use system events as a driver, high real-time performance
 * Pass the event to the observer, which scans the folder to further determine the specific type of event event
 */
public class FileEventListener implements JNotifyListener {

    private FileAlterationObserver observer;

    /**
     * constructor
     *
     * @param observer Watchers of a folder event
     */
    public FileEventListener(FileAlterationObserver observer) {
        this.observer = observer;
    }

    /**
     * File rename event callback interface
     */
    @Override
    public void fileRenamed(int wd, String rootPath, String oldName,
                            String newName) {
        observer.checkAndNotify();
    }

    /**
     * File modification event callback interface
     */
    @Override
    public void fileModified(int wd, String rootPath, String name) {
        observer.checkAndNotify();
    }

    /**
     * File file deletion event callback interface
     */
    @Override
    public void fileDeleted(int wd, String rootPath, String name) {
        observer.checkAndNotify();
    }

    /**
     * File creation event callback interface
     */
    @Override
    public void fileCreated(int wd, String rootPath, String name) {
        observer.checkAndNotify();
    }

}
