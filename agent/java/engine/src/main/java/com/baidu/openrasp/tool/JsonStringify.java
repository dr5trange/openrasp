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

/**
 * Created by tyy on 7/6/17.
 * json string formatting tool class
 */
public class JsonStringify {

    /**
     * Handling special characters in strings for escaping
     * @param jsonString pending string
     * @return processed string
     */
    public static String stringify(String jsonString) {
        StringBuffer tmpString = new StringBuffer();
        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);
            switch (c) {
                case '\"':
                    tmpString.append("\\\"");
                    break;
                case '\\':
                    tmpString.append("\\\\");
                    break;
                case '/':
                    tmpString.append("\\/");
                    break;
                case '\b':
                    tmpString.append("\\b");
                    break;
                case '\f':
                    tmpString.append("\\f");
                    break;
                case '\n':
                    tmpString.append("\\n");
                    break;
                case '\r':
                    tmpString.append("\\r");
                    break;
                case '\t':
                    tmpString.append("\\t");
                    break;
                default:
                    tmpString.append(c);
            }
        }
        return tmpString.toString();
    }

}
