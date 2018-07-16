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

import com.baidu.openrasp.TokenGenerator;
import com.baidu.openrasp.plugin.antlrlistener.TokenizeErrorListener;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Java implementation of token parsing, will be registered to the RASP object in JS
 */
public class JSTokenizeSql extends BaseFunction {
    private static TokenizeErrorListener tokenizeErrorListener = new TokenizeErrorListener();
    /**
     * @see BaseFunction#call(Context, Scriptable, Scriptable, Object[])
     * @param cx
     * @param scope
     * @param thisObj
     * @param args
     * @return
     */
    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                       Object[] args) {
        if (args.length < 1) {
            return Context.getUndefinedValue();
        }
        if (!(args[0] instanceof String)) {
            return Context.getUndefinedValue();
        }
        String sql = (String) args[0];
        String[] result = TokenGenerator.tokenize(sql, tokenizeErrorListener);
        int length = result.length;
        Scriptable array = cx.newArray(scope, length);
        for (int i = 0; i < length; i++) {
            array.put(i, array, result[i]);
        }
        return array;
    }

    /**
     * Provides a way to get the default value of the object
     * console.log(thisObj) will output the value returned by this method
     * @see Scriptable#getDefaultValue(Class)
     * @param hint
     * @return
     */
    @Override
    public Object getDefaultValue(Class<?> hint) {
        return "[Function: sql_tokenize]";
    }
}
