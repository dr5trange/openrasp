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

import java.lang.instrument.Instrumentation;

/**
 * Created by tyy on 18-2-1.
 *
 * Modules that need to be inherited for each submodule entry
 * Module entry class configuration in the MANIFEST configuration of the submodule jar package
 */
public interface Module {

    void start(String agentArg, Instrumentation inst) throws Exception;

    void release();

}
