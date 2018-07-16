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

package com.baidu.openrasp.tool;

import com.baidu.openrasp.EngineBoot;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by tyy on 4/7/17.
 * File Tools
 */
public class FileUtil {
    /**
     * Read file contents
     *
     * @param file file object
     * @return file string content
     * @throws IOException {@link IOException}
     */
    public static String readFileByFile(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }

    /**
     * Restore the real path of the file to avoid bypassing
     *
     * @param file file
     * @return real file path su
     */
    public static String getRealPath(File file) {
        String absPath = file.getAbsolutePath();
        if (OSUtil.isWindows()) {
            int index = absPath.indexOf("::$");
            if (index >= 0) {
                file = new File(absPath.substring(0, index));
            }
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return absPath;
        }
    }

    /**
     * Get the folder path where the current jar package is located
     *
     * @return jar package folder path
     */
    public static String getBaseDir() {
        String baseDir;
        String jarPath = EngineBoot.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            baseDir = URLDecoder.decode(new File(jarPath).getParent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            baseDir = new File(jarPath).getParent();
        }
        return baseDir;
    }

}
