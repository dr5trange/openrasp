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

package com.baidu.openrasp;

import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.exception.SecurityException;
import com.baidu.openrasp.hook.XXEHook;
import com.baidu.openrasp.plugin.checker.CheckParameter;
import com.baidu.openrasp.plugin.checker.CheckerManager;
import com.baidu.openrasp.plugin.js.engine.JSContext;
import com.baidu.openrasp.request.AbstractRequest;
import com.baidu.openrasp.request.HttpServletRequest;
import com.baidu.openrasp.response.HttpServletResponse;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhuming01 on 5/16/17.
 * All rights reserved
 * In the hook increase the detection point of the hook point processing class
 */
@SuppressWarnings("unused")
public class HookHandler {
    public static final String OPEN_RASP_HEADER_KEY = "X-Protected-By";
    public static final String OPEN_RASP_HEADER_VALUE = "OpenRASP";
    public static final String REQUEST_ID_HEADER_KEY = "X-Request-ID";
    public static final Logger LOGGER = Logger.getLogger(HookHandler.class.getName());
    // global switch
    public static AtomicBoolean enableHook = new AtomicBoolean(false);
    // current thread switch
    private static ThreadLocal<Boolean> enableCurrThreadHook = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static ThreadLocal<Boolean> tmpEnableCurrThreadHook = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static ThreadLocal<AbstractRequest> requestCache = new ThreadLocal<AbstractRequest>() {
        @Override
        protected AbstractRequest initialValue() {
            return null;
        }
    };
    public static ThreadLocal<HttpServletResponse> responseCache = new ThreadLocal<HttpServletResponse>() {
        @Override
        protected HttpServletResponse initialValue() {
            return null;
        }
    };

    private static final Map<String, Object> EMPTY_MAP = new HashMap<String, Object>();

    /**
     * used to close the current thread's hook point
     */
    public static void disableCurrThreadHook() {
        enableCurrThreadHook.set(false);
    }

    /**
     * used to open the current thread's hook point
     */
    public static void enableCurrThreadHook() {
        enableCurrThreadHook.set(true);
    }

    public static boolean isEnableCurrThreadHook() {
        return enableCurrThreadHook.get();
    }

    /**
     * Used to test new hook points and use hook information as log print
     *
     * @param className hook point class name
     * @param method hook method name
     * @param desc hook point method descriptor
     * @param args hook point parameter list
     */
    public static void checkCommon(String className, String method, String desc, Object... args) {
        LOGGER.debug("checkCommon: " + className + ":" + method + ":" + desc + " " + Arrays.toString(args));
    }

    /**
     * Enter the method that needs to block the hook to close the switch
     */
    public static void preShieldHook() {
        tmpEnableCurrThreadHook.set(enableCurrThreadHook.get());
        disableCurrThreadHook();
    }

    /**
     * Exit the method that needs to block the hook to open the switch
     */
    public static void postShieldHook() {
        if (tmpEnableCurrThreadHook.get()) {
            enableCurrThreadHook();
        }
    }

    /**
     * Request to enter the hook point
     *
     * @param servlet servlet object
     * @param request request entity
     * @param response response entity
     */
    public static void checkRequest(Object servlet, Object request, Object response) {
        if (servlet != null && request != null && !enableCurrThreadHook.get()) {
            // The default is to turn off the hook, only the thread that has processed the HTTP request will open.
            enableCurrThreadHook.set(true);
            HttpServletRequest requestContainer = new HttpServletRequest(request);
            HttpServletResponse responseContainer = new HttpServletResponse(response);
            responseContainer.setHeader(OPEN_RASP_HEADER_KEY, OPEN_RASP_HEADER_VALUE);
            responseContainer.setHeader(REQUEST_ID_HEADER_KEY, requestContainer.getRequestId());
            requestCache.set(requestContainer);
            responseCache.set(responseContainer);
            XXEHook.resetLocalExpandedSystemIds();
            doCheck(CheckParameter.Type.REQUEST, JSContext.getUndefinedValue());
        }
    }

    /**
     * Request to end the hook point
     * You cannot enter any hook point after the request ends.
     */
    public static void onServiceExit() {
        enableCurrThreadHook.set(false);
        requestCache.set(null);
    }

    /**
     * Hook points entered in the filter
     *
     * @param filter filter
     * @param request request entity
     * @param response response entity
     */
    public static void checkFilterRequest(Object filter, Object request, Object response) {
        checkRequest(filter, request, response);
    }

    public static void onInputStreamRead(int ret, Object inputStream) {
        if (ret != -1 && requestCache.get() != null) {
            AbstractRequest request = requestCache.get();
            if (request.getInputStream() == null) {
                request.setInputStream(inputStream);
            }
            if (request.getInputStream() == inputStream) {
                request.appendBody(ret);
            }
        }
    }

    public static void onInputStreamRead(int ret, Object inputStream, byte[] bytes) {
        if (ret != -1 && requestCache.get() != null) {
            AbstractRequest request = requestCache.get();
            if (request.getInputStream() == null) {
                request.setInputStream(inputStream);
            }
            if (request.getInputStream() == inputStream) {
                request.appendBody(bytes, 0, ret);
            }
        }
    }

    public static void onInputStreamRead(int ret, Object inputStream, byte[] bytes, int offset, int len) {
        if (ret != -1 && requestCache.get() != null) {
            AbstractRequest request = requestCache.get();
            if (request.getInputStream() == null) {
                request.setInputStream(inputStream);
            }
            if (request.getInputStream() == inputStream) {
                request.appendBody(bytes, offset, ret);
            }
        }
    }

    public static void onParseParameters() {
        AbstractRequest request = requestCache.get();
        if (request != null) {
            request.setCanGetParameter(true);
        }
    }

    private static void handleBlock() {
        SecurityException securityException = new SecurityException("Request blocked by OpenRASP");
        if (responseCache.get() != null) {
            responseCache.get().sendError();
        }
        throw securityException;
    }

    /**
     * No need to detect the execution of the entry in the request thread
     *
     * @param type detection type
     * @param params detection parameter map, key is the parameter name, value is the detection parameter value
     */
    public static void doCheckWithoutRequest(CheckParameter.Type type, Object params) {
        long a = 0;
        if (Config.getConfig().getDebugLevel() > 0) {
            a = System.currentTimeMillis();
        }
        boolean enableHookCache = enableCurrThreadHook.get();
        boolean isBlock = false;
        try {
            enableCurrThreadHook.set(false);
            CheckParameter parameter = new CheckParameter(type, params);
            isBlock = CheckerManager.check(type, parameter);
        } catch (Exception e) {
            LOGGER.warn("plugin check error: " + e.getClass().getName()
                    + " because: " + e.getMessage() + " stacktrace: " + e.getStackTrace());
        } finally {
            enableCurrThreadHook.set(enableHookCache);
        }
        if (a > 0) {
            long t = System.currentTimeMillis() - a;
            if (requestCache.get() != null) {
                LOGGER.info("request_id=" + requestCache.get().getRequestId() + " " + "type=" + type.getName() + " " + "time=" + t);
            }
        }
        if (isBlock) {
            handleBlock();
        }
    }

    /**
     * Request thread detection entry
     *
     * @param type detection type
     * @param params detection parameter map, key is the parameter name, value is the detection parameter value
     */
    public static void doCheck(CheckParameter.Type type, Object params) {
        if (enableHook.get() && enableCurrThreadHook.get()) {
            doCheckWithoutRequest(type, params);
        }
    }

}
