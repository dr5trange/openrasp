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

package com.baidu.rasp.uninstall;

import com.baidu.rasp.RaspError;
import com.baidu.rasp.install.BaseStandardInstaller;

import java.io.File;
import java.io.IOException;

import static com.baidu.rasp.RaspError.E10003;
import static com.baidu.rasp.RaspError.E10002;

/**
 * @Description: Automatically uninstall base class
 * @author anyang
 * @date 2018/4/25 19:37
 */
public abstract class BaseStandardUninstaller implements Uninstaller {
    private String serverRoot;
    private String serverName;
    public static String LINE_SEP = System.getProperty("line.separator");

    public BaseStandardUninstaller(String serverName, String serverRoot) {
        this.serverRoot = serverRoot;
        this.serverName = serverName;
    }

    @Override
    public void uninstall() throws RaspError, IOException {
        File installDir = new File(getInstallPath(serverRoot));
        BaseStandardInstaller.modifyFolerPermission(installDir.getCanonicalPath());
        // Delete Files
        delRaspFolder(getInstallPath(serverRoot));
        // Find the startup script to modify
        File script = new File(getScript(installDir.getPath()));
        if (!script.exists()) {
            throw new RaspError(E10003 + script.getAbsolutePath());
        }

        // Restore the script modified during installation
        System.out.println("Updating startup script\n- " + script.getCanonicalPath());
        String original = BaseStandardInstaller.read(script);
        String modified = recoverStartScript(original);
        BaseStandardInstaller.write(script, modified);
        System.out.println("\nUninstallation completed without errors.\nPlease restart application server to take effect.");

    }

    public void delAllFile(String path) throws RaspError {
        File file = new File(path);
        String[] tempList = file.list();
        File temp = null;
        for (String s : tempList) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + s);
            } else {
                temp = new File(path + File.separator + s);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                // First delete the files inside the folder
                delAllFile(path + File.separator + s);
                // delete the empty folder again
                delRaspFolder(path + File.separator + s);
            }
        }
    }

    public void delRaspFolder(String folderPath) throws RaspError {

        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new RaspError(E10002 + folderPath);
        }
        // Delete all files and subfolders in the rasp folder
        delAllFile(folderPath);
        // Delete rasp empty folder
        folder.delete();

    }

    protected abstract String getInstallPath(String serverRoot);

    protected abstract String getScript(String installPath);

    protected abstract String recoverStartScript(String content) throws RaspError;
}
