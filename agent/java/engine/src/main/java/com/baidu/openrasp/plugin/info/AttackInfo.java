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

package com.baidu.openrasp.plugin.info;

import com.baidu.openrasp.plugin.checker.CheckParameter;
import com.baidu.openrasp.request.AbstractRequest;
import com.baidu.openrasp.tool.OSUtil;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuming01 on 7/11/17.
 * All rights reserved
 * Attack information class, mainly used for alarms
 *
 * @see <a href="https://rasp.baidu.com/doc/setup/log/main.html">document</a>
 */
public class AttackInfo extends EventInfo {
    public static final String TYPE_ATTACK = "attack";
    public static final String DEFAULT_LOCAL_PLUGIN_NAME = "java_builtin_plugin";

    public static final int DEFAULT_CONFIDENCE_VALUE = 100;

    private CheckParameter parameter;
    private String pluginName;
    private String message;
    private String action;
    private int confidence;

    public static AttackInfo createLocalAttackInfo(CheckParameter parameter, String action, String message) {
        return new AttackInfo(parameter, action, message, DEFAULT_LOCAL_PLUGIN_NAME);
    }

    public static AttackInfo createLocalAttackInfo(CheckParameter parameter, String action, String message, int confidence) {
        return new AttackInfo(parameter, action, message, DEFAULT_LOCAL_PLUGIN_NAME, confidence);
    }

    public AttackInfo(CheckParameter parameter, String action, String message, String pluginName) {
        this(parameter, action, message, pluginName, DEFAULT_CONFIDENCE_VALUE);
    }

    public AttackInfo(CheckParameter parameter, String action, String message, String pluginName, int confidence) {
        this.message = message;
        this.pluginName = pluginName;
        this.action = action;
        this.confidence = confidence;
        this.parameter = parameter;
        setBlock(CHECK_ACTION_BLOCK.equals(action));
    }

    /**
     * Organize information about attack requests
     *
     * @return attack information
     */
    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<String, Object>();
        AbstractRequest request = parameter.getRequest();
        Timestamp createTime = new Timestamp(parameter.getCreateTime());

        info.put("event_type", getType());
        // attack time
        info.put("event_time", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(createTime));
        // server host name
        info.put("server_hostname", OSUtil.getHostName());
        // attack type
        info.put("attack_type", parameter.getType().toString());
        // attack parameters
        info.put("attack_params", parameter.getParams());
        // attack the call stack
        StackTraceElement[] trace = filter(new Throwable().getStackTrace());
        info.put("stack_trace", stringify(trace));
        // Detect plugin
        info.put("plugin_name", this.pluginName);
        // plugin message
        info.put("plugin_message", this.message);
        // plugin confidence
        info.put("plugin_confidence", this.confidence);
        // Whether to intercept
        info.put("intercept_state", this.action);

        if (request != null) {
            // request ID
            info.put("request_id", request.getRequestId());
            // Attack source IP
            info.put("attack_source", request.getRemoteAddr());
            // Target domain name being attacked
            info.put("target", request.getServerName());
            // attacked target IP
            info.put("server_ip", request.getLocalAddr());
            // Target server type and version being attacked
            Map<String, String> serverInfo = request.getServerContext();
            info.put("server_type", serverInfo != null ? serverInfo.get("server") : null);
            info.put("server_version", serverInfo != null ? serverInfo.get("version") : null);
            // Attacked URL
            StringBuffer requestURL = request.getRequestURL();
            String queryString = request.getQueryString();
            info.put("url", requestURL == null ? "" : (queryString != null ? requestURL + "?" + queryString : requestURL));
            // request body
            byte[] requestBody = request.getBody();
            if (requestBody != null) {
                info.put("body", new String(requestBody));
            }
            // was attacked PATH
            info.put("path", request.getRequestURI());
            // User agent
            info.put("user_agent", request.getHeader("User-Agent"));
            // The Referrer's Referrer Head
            String referer = request.getHeader("Referer");
            info.put("referer", referer == null ? "" : referer);
        }

        return info;
    }

    @Override
    public String getType() {
        return TYPE_ATTACK;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
