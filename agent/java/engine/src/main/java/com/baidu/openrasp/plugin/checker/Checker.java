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

package com.baidu.openrasp.plugin.checker;

import org.apache.log4j.Logger;

/**
 * Created by tyy on 17-11-22.
 *
 * hook point detection interface
 */
public interface Checker {

    Logger POLICY_ALARM_LOGGER = Logger.getLogger(AbstractChecker.class.getPackage().getName() + ".policy_alarm");

    Logger ATTACK_ALARM_LOGGER = Logger.getLogger(AbstractChecker.class.getPackage().getName() + ".alarm");

    /**
     * Detect hook parameter
     *
     * @param parameter hook parameter
     * @return Whether blocking true means safe false means dangerous
     */
    boolean check(CheckParameter parameter);

}
