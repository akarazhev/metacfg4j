/* Copyright 2019 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.db;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.engine.db.h2db.H2dbServer;

import java.sql.SQLException;

/**
 * Provides factory methods to create a db server.
 */
public final class DbServers {

    private DbServers() {
        // Factory class
    }

    /**
     * Returns a default db server.
     *
     * @return a db server.
     */
    public static DbServer newServer() throws SQLException {
        return new H2dbServer();
    }

    /**
     * Returns a db server based on the configuration.
     *
     * @param config config a configuration of a db server.
     * @return a db server.
     */
    public static DbServer newServer(final Config config) throws SQLException {
        return new H2dbServer(config) ;
    }
}
