/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.db;

import java.sql.SQLException;

/**
 * Provides basic methods for a db server.
 */
public interface DbServer {
    /**
     * Starts a db server and returns an instance.
     *
     * @return a db server.
     * @throws SQLException when a db server encounters a problem.
     */
    DbServer start() throws SQLException;

    /**
     * Stops a db server
     */
    void stop();
}
