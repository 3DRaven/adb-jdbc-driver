/*******************************************************************************
 * Copyright 2021 Renat Eskenin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.anyqn.adbjdbc;

import java.util.Map;
import java.util.TimerTask;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public abstract class AbstractFetchingTask extends TimerTask {
    @Getter
    private final JadbConnection jadb;
    @Getter
    private final String deviceIdentifier;
    @Getter
    private final @NonNull Map<String, String> params;
    @Getter
    private final @NonNull String packageName;

    public AbstractFetchingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            @NonNull final Map<String, String> params, @NonNull final JadbConnection jadb) {
        log.debug("Called");
        this.packageName = packageName;
        this.params = params;
        this.jadb = jadb;
        this.deviceIdentifier = deviceIdentifier;
    }

}
