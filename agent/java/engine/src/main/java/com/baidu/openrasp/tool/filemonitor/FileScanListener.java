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

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

/**
 * Created by tyy on 4/18/17.
 * Listener for the file change event triggered by the observer scan folder
 * Blocked folder events
 */
public abstract class FileScanListener implements FileAlterationListener {

    /**
     * Observers begin to observe the callback interface
     *
     * @param fileAlterationObserver Observer object
     */
    @Override
    public void onStart(FileAlterationObserver fileAlterationObserver) {

    }

    /**
     * Folder creation event
     *
     * @param file folder object
     */
    @Override
    public void onDirectoryCreate(File file) {

    }

    /**
     * Folder change event
     *
     * @param file folder event
     */
    @Override
    public void onDirectoryChange(File file) {

    }

    /**
     * Folder deletion event
     *
     * @param file folder object
     */
    @Override
    public void onDirectoryDelete(File file) {

    }

    /**
     * File creation event
     *
     * @param file file object
     */
    public abstract void onFileCreate(File file);

    /**
     * File change event
     *
     * @param file file object
     */
    public abstract void onFileChange(File file);

    /**
     * File deletion event
     *
     * @param file file object
     */
    public abstract void onFileDelete(File file);

    /**
     * Observer ends the observation event
     *
     * @param fileAlterationObserver Observer object
     */
    @Override
    public void onStop(FileAlterationObserver fileAlterationObserver) {

    }
}
