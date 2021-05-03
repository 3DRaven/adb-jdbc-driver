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
package com.anyqn.adbjdbc.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerWithException<Target, ExObj extends Exception> {
    void accept(Target target) throws ExObj;

    static <Target> Consumer<Target> handlerBuilder(final ConsumerWithException<Target, Exception> handlingConsumer) {
        return obj -> {
            try {
                handlingConsumer.accept(obj);
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}
