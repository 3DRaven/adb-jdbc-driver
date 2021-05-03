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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ConnectionHolder {

    private Connection connection;
    private final Semaphore attachBlocker = new Semaphore(1, true);

    public synchronized void acquire() throws InterruptedException {
        log.info("Acquired semapgore");
        attachBlocker.acquire();
    }

    public synchronized void close() throws InterruptedException, SQLException {
        if (null != connection) {
            connection.close();
        }
    }

    private Semaphore getAttachBlocker() {
        return attachBlocker;
    }

    public synchronized Connection getConnection() {
        return connection;
    }

    public synchronized boolean isExists() throws SQLException, InterruptedException {
        final boolean exists = connection != null && !connection.isClosed();
        return exists;
    }

    public synchronized void release() {
        log.info("Released semapgore");
        attachBlocker.release();
    }

    public void waitConnection() {
        while (null == connection) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
            }
        }
    }
}
