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

package com.baidu.openrasp.response;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.tool.Reflection;

/**
 * Created by tyy on 9/5/17.
 * A unified interface for javax.servlet.http.HttpServletResponse type response
 */
public class HttpServletResponse {

    private static final int REDIRECT_STATUS_CODE = 302;
    public static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER_KEY = "Content-Length";
    public static final String CONTENT_TYPE_HTML_VALUE = "text/html";

    private Object response;

    /**
     * constructor
     *
     * @param response http response entity
     */
    public HttpServletResponse(Object response) {
        this.response = response;
    }

    /**
     * Get http corresponding entity
     *
     * @return http response entity
     */
    public Object getResponse() {
        return response;
    }

    /**
     * Set the response header to override the original value
     *
     * @param key response header name
     * @param value response header value
     */
    public void setHeader(String key, String value) {
        if (response != null) {
            Reflection.invokeMethod(response, "setHeader", new Class[]{String.class, String.class}, key, value);
        }
    }

    /**
     * Set the digital response header to cover the original value
     *
     * @param key response header name
     * @param value response header value
     */
    public void setIntHeader(String key, int value) {
        if (response != null) {
            Reflection.invokeMethod(response, "setIntHeader", new Class[]{String.class, int.class}, key, value);
        }
    }

    /**
     * Set response headers, not overwritten
     *
     * @param key response header name
     * @param value response header value
     */
    public void addHeader(String key, String value) {
        if (response != null) {
            Reflection.invokeMethod(response, "addHeader", new Class[]{String.class, String.class}, key, value);
        }
    }

    /**
     * Get response header
     *
     * @param key response header name
     * @return response header value
     */
    public String getHeader(String key) {
        if (response != null) {
            Object header = Reflection.invokeMethod(response, "getHeader", new Class[]{String.class}, key);
            if (header != null) {
                return header.toString();
            }
        }
        return null;
    }

    public String getContentType() {
        if (response != null) {
            Object contentType = Reflection.invokeMethod(response, "getContentType", new Class[]{});
            if (contentType != null) {
                return contentType.toString();
            }
        }
        return null;
    }

    /**
     * Clear all body buffer cache
     *
     * @return is successful
     */
    public boolean resetBuffer() {
        if (response != null) {
            try {
                Reflection.invokeMethod(response, "resetBuffer", new Class[]{});
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Clear all buffer caches
     *
     * @return is successful
     */
    public boolean reset() {
        if (response != null) {
            try {
                Reflection.invokeMethod(response, "reset", new Class[]{});
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Return exception information
     */
    public void sendError() {
        if (response != null) {
            try {
                int statusCode = Config.getConfig().getBlockStatusCode();
                String blockUrl = Config.getConfig().getBlockUrl();
                boolean isCommitted = (Boolean) Reflection.invokeMethod(response, "isCommitted", new Class[]{});
                if (!blockUrl.contains("?")) {
                    String blockParam = "?request_id=" + HookHandler.requestCache.get().getRequestId();
                    blockUrl += blockParam;
                }
                String script = "</script><script>location.href=\"" + blockUrl + "\"</script>";
                if (!isCommitted) {
                    Reflection.invokeMethod(response, "setStatus", new Class[]{int.class}, statusCode);
                    if (statusCode >= 300 && statusCode <= 399) {
                        setHeader("Location", blockUrl);
                    }
                    setIntHeader(CONTENT_LENGTH_HEADER_KEY, script.getBytes().length);
                }
                resetBuffer();
                sendContent(script, true);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    /**
     * Send custom error handling scripts
     */
    public void sendContent(String content, boolean close) {
        Object printer = null;

        printer = Reflection.invokeMethod(response, "getWriter", new Class[]{});
        if (printer == null) {
            printer = Reflection.invokeMethod(response, "getOutputStream", new Class[]{});
        }
        Reflection.invokeMethod(printer, "print", new Class[]{String.class}, content);
        Reflection.invokeMethod(printer, "flush", new Class[]{});
        if (close) {
            Reflection.invokeMethod(printer, "close", new Class[]{});
        }
    }

}
