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

package com.baidu.openrasp.tool.hook;

/**
 * Created by lxk on 6/11/17.
 * Used to record file information during the write operation of hook FileOutputStream
 * is using a lock member variable in FileOutputStream
 * Use the object record information in the constructor hook point of the file information recorded during the write operation of FileOutputStream
 */
public class CustomLockObject {
    private String info = null;

    /**
     * constructor
     */
    public CustomLockObject() {
    }

    /**
     * Get file information
     *
     * @return file information
     */
    public String getInfo() {
        return info;
    }

    /**
     * Set file information
     *
     * @param info file information
     */
    public void setInfo(String info) {
        this.info = info;
    }
}
