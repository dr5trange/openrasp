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

package com.baidu.openrasp.hook;


import com.baidu.openrasp.config.Config;
import javassist.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by zhuming01 on 5/19/17.
 * All rights reserved
 *
 * Classes used to hook hooks to fixed methods of fixed classes
 * Different hook points implement the abstract class according to their own needs
 */
public abstract class AbstractClassHook {

    private static final Logger LOGGER = Logger.getLogger(AbstractClassHook.class.getName());

    protected boolean couldIgnore = true;

    private boolean isLoadedByBootstrapLoader = false;

    /**
     * Used to determine whether the class name is the same as the class that currently needs the hook
     *
     * @param className class name to use for matching
     * @return matches
     */
    public abstract boolean isClassMatched(String className);

    /**
     * The type of detection that the hook point belongs to.
     *
     * @return detection type
     * @see <a href="https://rasp.baidu.com/doc/dev/data.html">https://rasp.baidu.com/doc/dev/data.html</a>
     */
    public abstract String getType();

    /**
     * hook function of the target class
     *
     * @param ctClass target class
     */
    protected abstract void hookMethod(CtClass ctClass) throws IOException, CannotCompileException, NotFoundException;

    /**
     * Conversion target class
     *
     * @param ctClass class to be converted
     * @return bytecode array of the class after conversion
     */
    public byte[] transformClass(CtClass ctClass) {
        try {
            hookMethod(ctClass);
            return ctClass.toBytecode();
        } catch (Exception e) {
            if (Config.getConfig().isDebugEnabled()) {
                LOGGER.error("transform class " + ctClass.getName() + " failed", e);
            }
        }
        return null;
    }

    /**
     * Can it be ignored in the hook.ignore configuration item
     *
     * @return hook points can not be ignored
     */
    public boolean couldIgnore() {
        return couldIgnore;
    }

    /**
     * Whether the class where the hook point is loaded by BootstrapClassLoader
     *
     * @return true means yes
     */
    public boolean isLoadedByBootstrapLoader() {
        return isLoadedByBootstrapLoader;
    }

    /**
     * Set whether the class of the hook point is loaded by BootstrapClassLoader
     *
     * @param loadedByBootstrapLoader true means yes
     */
    public void setLoadedByBootstrapLoader(boolean loadedByBootstrapLoader) {
        isLoadedByBootstrapLoader = loadedByBootstrapLoader;
    }

    /**
     * Insert the appropriate source code at the entry of the target method of the target class
     *
     * @param ctClass target class
     * @param methodName target method name
     * @param desc description of the target method
     * @param src source code to be inserted
     */
    public void insertBefore(CtClass ctClass, String methodName, String desc, String src)
            throws NotFoundException, CannotCompileException {

        LinkedList<CtBehavior> methods = getMethod(ctClass, methodName, desc);
        if (methods != null && methods.size() > 0) {
            for (CtBehavior method : methods) {
                if (method != null) {
                    insertBefore(method, src);
                }
            }
        } else {
            if (Config.getConfig().isDebugEnabled()) {
                LOGGER.warn("can not find method " + methodName + " " + desc + " in class " + ctClass.getName());
            }
        }

    }

    /**
     * Insert the appropriate source code at the entry of a set of overloaded target methods of the target class
     *
     * @param ctClass target class
     * @param methodName target method name
     * @param allDesc A set of descriptors for the target method
     * @param src source code to be inserted
     */
    public void insertBefore(CtClass ctClass, String methodName, String src, String[] allDesc)
            throws NotFoundException, CannotCompileException {
        for (String desc : allDesc) {
            insertBefore(ctClass, methodName, desc, src);
        }
    }

    /**
     * Insert the appropriate source code at the exit of the target method of the target class
     *
     * @param ctClass target class
     * @param methodName target method name
     * @param desc description of the target method
     * @param src source code to be inserted
     * @param asFinally Whether to execute the source code when an exception is thrown
     */
    public void insertAfter(CtClass ctClass, String methodName, String desc, String src, boolean asFinally)
            throws NotFoundException, CannotCompileException {

        LinkedList<CtBehavior> methods = getMethod(ctClass, methodName, desc);
        if (methods != null && methods.size() > 0) {
            for (CtBehavior method : methods) {
                if (method != null) {
                    insertAfter(method, src, asFinally);
                }
            }
        } else {
            if (Config.getConfig().isDebugEnabled()) {
                LOGGER.warn("can not find method " + methodName + " " + desc + " in class " + ctClass.getName());
            }
        }

    }

    private LinkedList<CtBehavior> getConstructor(CtClass ctClass, String desc) {
        LinkedList<CtBehavior> methods = new LinkedList<CtBehavior>();
        if (StringUtils.isEmpty(desc)) {
            Collections.addAll(methods, ctClass.getDeclaredConstructors());
        } else {
            try {
                methods.add(ctClass.getConstructor(desc));
            } catch (NotFoundException e) {
                // ignore
            }
        }
        return methods;
    }

    /**
     * Get a method instance of a specific class
     * If the descriptor is empty, then return all methods with the same name
     *
     * @param ctClass javassist class instance
     * @param methodName method name
     * @param desc method descriptor
     * @return All method instances that meet the requirements
     * @see javassist.bytecode.Descriptor
     */
    private LinkedList<CtBehavior> getMethod(CtClass ctClass, String methodName, String desc) {
        if ("<init>".equals(methodName)) {
            return getConstructor(ctClass, desc);
        }
        LinkedList<CtBehavior> methods = new LinkedList<CtBehavior>();
        if (StringUtils.isEmpty(desc)) {
            CtMethod[] allMethods = ctClass.getDeclaredMethods();
            if (allMethods != null) {
                for (CtMethod method : allMethods) {
                    if (method != null && !method.isEmpty() && method.getName().equals(methodName))
                        methods.add(method);
                }
            }
        } else {
            try {
                CtMethod ctMethod = ctClass.getMethod(methodName, desc);
                if (ctMethod != null && !ctMethod.isEmpty()) {
                    methods.add(ctClass.getMethod(methodName, desc));
                }
            } catch (NotFoundException e) {
                // ignore
            }
        }
        return methods;
    }

    /**
     * Insert the appropriate source code at the entry of the target method of the target class
     *
     * @param method target method
     * @param src source code
     */
    public void insertBefore(CtBehavior method, String src) throws CannotCompileException {
        try {
            method.insertBefore(src);
            LOGGER.info("insert before method " + method.getLongName());
        } catch (CannotCompileException e) {
            if (Config.getConfig().isDebugEnabled()) {
                LOGGER.error("insert before method " + method.getLongName() + " failed", e);
            }
            throw e;
        }
    }

    /**
     * (none-javadoc)
     *
     * @see com.baidu.openrasp.hook.AbstractClassHook#insertAfter(CtClass, String, String, String, boolean)
     */
    public void insertAfter(CtClass invokeClass, String methodName, String desc, String src)
            throws NotFoundException, CannotCompileException {
        insertAfter(invokeClass, methodName, desc, src, false);
    }

    /**
     * Insert the appropriate source code at the exit of the target method of the target class
     *
     * @param method target method
     * @param src source code
     * @param asFinally Whether to execute the source code when an exception is thrown
     */
    public void insertAfter(CtBehavior method, String src, boolean asFinally) throws CannotCompileException {
        try {
            method.insertAfter(src, asFinally);
            LOGGER.info("insert after method: " + method.getLongName());
        } catch (CannotCompileException e) {
            LOGGER.error("insert after method " + method.getLongName() + " failed", e);
            throw e;
        }
    }

    /**
     * Get the code string that calls the static method
     *
     * @param invokeClass The class to which the static method belongs
     * @param methodName static method name
     * @param paramString calls the passed argument string, in javassist format
     * @return code after integration
     */
    public String getInvokeStaticSrc(Class invokeClass, String methodName, String paramString, Class... parameterTypes) {
        String src;
        String invokeClassName = invokeClass.getName();

        String parameterTypesString = "";
        if (parameterTypes != null && parameterTypes.length > 0) {
            for (Class parameterType : parameterTypes) {
                if (parameterType.getName().startsWith("[")) {
                    parameterTypesString += "Class.forName(\"" + parameterType.getName() + "\"),";
                } else {
                    parameterTypesString += (parameterType.getName() + ".class,");
                }
            }
            parameterTypesString = parameterTypesString.substring(0, parameterTypesString.length() - 1);
        }
        if (parameterTypesString.equals("")) {
            parameterTypesString = null;
        } else {
            parameterTypesString = "new Class[]{" + parameterTypesString + "}";
        }
        if (isLoadedByBootstrapLoader) {
            src = "com.baidu.openrasp.ModuleLoader.moduleClassLoader.loadClass(\"" + invokeClassName + "\").getMethod(\"" + methodName +
                    "\"," + parameterTypesString + ").invoke(null";
            if (!StringUtils.isEmpty(paramString)) {
                src += (",new Object[]{" + paramString + "});");
            } else {
                src += ",null);";
            }
            src = "try {" + src + "} catch (Throwable t) {if(t.getCause() != null && t.getCause().getClass()" +
                    ".getName().equals(\"com.baidu.openrasp.exception.SecurityException\")){throw t;}}";
        } else {
            src = invokeClassName + '.' + methodName + "(" + paramString + ");";
            src = "try {" + src + "} catch (Throwable t) {if(t.getClass()" +
                    ".getName().equals(\"com.baidu.openrasp.exception.SecurityException\")){throw t;}}";
        }
        return src;
    }

}
