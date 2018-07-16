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

import com.baidu.openrasp.transformer.CustomClassTransformer;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by tyy on 3/27/17.
 * All rights reserved
 * Reflection tool class
 */
public class Reflection {
    private static final Logger LOGGER = Logger.getLogger(Reflection.class.getName());

    /**
     * Calling a method of an object based on the method name
     *
     * @param object The object that called the method
     * @param methodName method name
     * @param paramTypes list of parameter types
     * @param parameters parameter list
     * @return method return value
     */
    public static Object invokeMethod(Object object, String methodName, Class[] paramTypes, Object... parameters) {
        if (object == null) {
            return null;
        }
        return invokeMethod(object, object.getClass(), methodName, paramTypes, parameters);
    }

    /**
     * Reflect the calling method and cast the return value to a String
     *
     * @return The String returned by the called function
     * @see #invokeMethod(Object, String, Class[], Object...)
     */
    public static String invokeStringMethod(Object object, String methodName, Class[] paramTypes, Object... parameters) {
        Object ret = invokeMethod(object, methodName, paramTypes, parameters);
        return ret != null ? (String) ret : null;
    }

    /**
     * The fields of the reflection get object include private
     *
     * @param object The object of the extracted field
     * @param fieldName field name
     * The value of the @return field
     */
    public static Object getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * Call a static method of a class
     *
     * @param className  Class name 

     * @param methodName method name
     * @param paramTypes list of parameter types
     * @param parameters parameter list
     * @return method return value
     */
    public static Object invokeStaticMethod(String className, String methodName, Class[] paramTypes, Object... parameters) {
        try {
            Class clazz = null;
            ClassLoader loader = CustomClassTransformer.getClassLoader(className);
            if (loader != null) {
                clazz = loader.loadClass(className);
            } else {
                clazz = Class.forName(className);
            }
            return invokeMethod(null, clazz, methodName, paramTypes, parameters);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invokeMethod(Object object, Class clazz, String methodName, Class[] paramTypes, Object... parameters) {
        try {
            Method method = clazz.getMethod(methodName, paramTypes);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(object, parameters);
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
        return null;
    }
}
