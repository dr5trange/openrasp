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

package com.baidu.openrasp.request;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.tool.Reflection;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * Created by zhuming01 on 6/23/17.
 * All rights reserved
 * Uniform format abstract class for different request hook points for different servers
 */
public abstract class AbstractRequest {
    protected static final Class[] EMPTY_CLASS = new Class[]{};
    protected static final Class[] STRING_CLASS = new Class[]{String.class};
    protected Object request;
    protected Object inputStream = null;
    protected ByteArrayOutputStream bodyOutputStream = null;
    protected CharArrayWriter bodyCharWriter = null;
    protected int maxBodySize = 4096;
    protected String requestId;
    protected boolean canGetParameter = false;

    /**
     * constructor
     *
     * @see AbstractRequest#AbstractRequest(Object) The default request entity is null
     */
    public AbstractRequest() {
        this(null);
    }

    /**
     * constructor
     *
     * @param request request entity
     */
    public AbstractRequest(Object request) {
        this.request = request;
        this.requestId = UUID.randomUUID().toString().replace("-", "");
        this.maxBodySize = Config.getConfig().getBodyMaxBytes();
    }

    /**
     * Returns whether the current request can get the parameter content
     *
     * @return can get the parameter content
     */
    public boolean isCanGetParameter() {
        return canGetParameter;
    }

    /**
     * Set whether you can get parameters
     *
     * @param canGetParameter Whether to get the parameter content
     */
    public void setCanGetParameter(boolean canGetParameter) {
        this.canGetParameter = canGetParameter;
    }

    /**
     * Set the request entity, which may be a different type in different environments
     *
     * @param request request entity
     */
    public void setRequest(Object request) {
        this.request = request;
    }

    /**
     * Get the request entity
     *
     * @return request entity
     */
    public Object getRequest() {
        return this.request;
    }

    /**
     * Get Request Id
     *
     * @return request Id
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Get this server address
     *
     * @return server address
     */
    public abstract String getLocalAddr();

    /**
     * Get request method
     *
     * @return request method
     */
    public abstract String getMethod();

    /**
     * Get the request agreement
     *
     * @return request protocol
     */
    public abstract String getProtocol();

    /**
     * Get verification type
     *
     * @return verification type
     */
    public abstract String getAuthType();

    /**
     * Get request path
     *
     * @return request path
     */
    public abstract String getContextPath();

    /**
     * Get the address of the access client
     *
     * @return client address
     */
    public abstract String getRemoteAddr();

    /**
     * Get the requested uri
     *
     * @return request uri
     */
    public abstract String getRequestURI();

    /**
     * Get the requested url
     *
     * @return request url
     */
    public abstract StringBuffer getRequestURL();

    /**
     * Get the server name
     *
     * @return server name
     */
    public abstract String getServerName();

    /**
     * Get the value of the request parameter based on the requested parameter name
     *
     * @param key request parameter name
     * @return request parameter value
     */
    public abstract String getParameter(String key);

    /**
     * Get all request parameter names
     *
     * @return request collection of parameter names
     */
    public abstract Enumeration<String> getParameterNames();

    /**
     * Get map key-value pair set of request parameters
     * key is the parameter name, value is the parameter value
     *
     * @return map collection of request parameters
     */
    public abstract Map<String, String[]> getParameterMap();

    /**
     * Get the value of the request header based on the name of the request header
     *
     * @param key request header name
     * @return request header value
     */
    public abstract String getHeader(String key);

    /**
     * Get the names of all request headers
     *
     * @return request enumeration collection of header names
     */
    public abstract Enumeration<String> getHeaderNames();

    /**
     * Get the Query String parameter part of the requested url
     *
     * @return Requested Query String
     */
    public abstract String getQueryString();

    /**
     * Get the server's context parameter map collection
     * key is the name of the parameter, and value is the value of the parameter.
     *
     * @return map collection of server context parameters
     */
    public abstract Map<String, String> getServerContext();

    /**
     * Get the app deployment root path
     *
     * @return app deployment root path
     */
    public abstract String getAppBasePath();

    /**
     * Return HTTP request body
     *
     * @return request body, can be null
     */
    public byte[] getBody() {
        return bodyOutputStream != null ? bodyOutputStream.toByteArray() : null;
    }

    /**
     * Return HTTP request body stream
     *
     * @return request body, can be null
     */
    public ByteArrayOutputStream getBodyStream() {
        return bodyOutputStream;
    }

    /**
     * return to input stream
     *
     * @return input stream
     */
    public Object getInputStream() {
        return inputStream;
    }

    /**
     * Set input stream
     *
     * @param inputStream input stream
     */
    public void setInputStream(Object inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Add to HTTP request body

     *
     * @param b bytes to be added
     */
    public void appendBody(int b) {
        if (bodyOutputStream == null) {
            bodyOutputStream = new ByteArrayOutputStream();
        }

        if (bodyOutputStream.size() < maxBodySize) {
            bodyOutputStream.write(b);
        }
    }

    /**
     * Add to HTTP request body

     *
     * @param bytes the byte array to be added
     */
    public void appendBody(byte[] bytes) {
        appendBody(bytes, 0, bytes.length);
    }

    /**
     * Add to HTTP request body

     *
     * @param bytes byte array
     * @param offset The starting offset to add
     * @param len length to be added
     */
    public void appendBody(byte[] bytes, int offset, int len) {
        if (bodyOutputStream == null) {
            bodyOutputStream = new ByteArrayOutputStream();
        }

        len = Math.min(len, maxBodySize - bodyOutputStream.size());
        if (len > 0) {
            bodyOutputStream.write(bytes, offset, len);
        }
    }

    protected boolean setCharacterEncodingFromConfig() {
        try {
            String paramEncoding = Config.getConfig().getRequestParamEncoding();
            if (!StringUtils.isEmpty(paramEncoding)) {
                Reflection.invokeMethod(request, "setCharacterEncoding", STRING_CLASS, paramEncoding);
                return true;
            }
        } catch (Exception e) {
            HookHandler.LOGGER.warn("set character encoding failed", e);
        }
        return false;
    }

}
