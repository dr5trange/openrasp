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

#include "openrasp_hook.h"

/**
 * command相关hook点
 */
PRE_HOOK_FUNCTION(passthru, command);
PRE_HOOK_FUNCTION(system, command);
PRE_HOOK_FUNCTION(exec, command);
PRE_HOOK_FUNCTION(shell_exec, command);
PRE_HOOK_FUNCTION(proc_open, command);
PRE_HOOK_FUNCTION(popen, command);
PRE_HOOK_FUNCTION(pcntl_exec, command);

PRE_HOOK_FUNCTION(passthru, webshell_command);
PRE_HOOK_FUNCTION(system, webshell_command);
PRE_HOOK_FUNCTION(exec, webshell_command);
PRE_HOOK_FUNCTION(shell_exec, webshell_command);
PRE_HOOK_FUNCTION(proc_open, webshell_command);
PRE_HOOK_FUNCTION(popen, webshell_command);
PRE_HOOK_FUNCTION(pcntl_exec, webshell_command);

static inline void openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAMETERS)
{
    zval *command;

    if (zend_parse_parameters(MIN(1, ZEND_NUM_ARGS()), "z", &command) != SUCCESS ||
        Z_TYPE_P(command) != IS_STRING)
    {
        return;
    }

    if (openrasp_zval_in_request(command))
    {
        zval attack_params;
        array_init(&attack_params);
        add_assoc_zval(&attack_params, "command", command);
        Z_ADDREF_P(command);
        zval plugin_message;
        ZVAL_STRING(&plugin_message, _("Webshell detected - Command execution backdoor"));
        openrasp_buildin_php_risk_handle(1, "webshell_command", 100, &attack_params, &plugin_message);
    }
}

static inline void openrasp_command_common(INTERNAL_FUNCTION_PARAMETERS)
{
    zend_string *command;

    if (zend_parse_parameters(MIN(1, ZEND_NUM_ARGS()), "S", &command) != SUCCESS)
    {
        return;
    }

    zval params;
    array_init(&params);
    add_assoc_str(&params, "command", command);
    zend_string_addref(command);
    zval stack;
    array_init(&stack);
    format_debug_backtrace_arr(&stack);
    add_assoc_zval(&params, "stack", &stack);
    check("command", &params);
}

void pre_global_passthru_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_passthru_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_system_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_system_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_exec_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_exec_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_shell_exec_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_shell_exec_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_proc_open_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_proc_open_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_popen_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_popen_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_pcntl_exec_webshell_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    openrasp_webshell_command_common(INTERNAL_FUNCTION_PARAM_PASSTHRU);
}

void pre_global_pcntl_exec_command(OPENRASP_INTERNAL_FUNCTION_PARAMETERS)
{
    zend_string *command;
    zval *args;

    if (zend_parse_parameters(MIN(2, ZEND_NUM_ARGS()), "S|a", &command, &args) != SUCCESS)
    {
        return;
    }

    if (ZEND_NUM_ARGS() > 1)
    {
        zend_string *delim = zend_string_init(" ", 1, 0);
        zval rst;
        php_implode(delim, args, &rst);
        zend_string_release(delim);
        if (Z_TYPE(rst) == IS_STRING)
        {
            zend_string *tmp = strpprintf(0, "%s %s", ZSTR_VAL(command), Z_STRVAL(rst));
            if (tmp)
            {
                command = tmp;
                zend_string_delref(command);
            }
        }
    }

    zval params;
    array_init(&params);
    add_assoc_str(&params, "command", command);
    zend_string_addref(command);
    zval stack;
    array_init(&stack);
    format_debug_backtrace_arr(&stack);
    add_assoc_zval(&params, "stack", &stack);
    check("command", &params);
}