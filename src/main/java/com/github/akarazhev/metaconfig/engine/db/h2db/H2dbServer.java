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
package com.github.akarazhev.metaconfig.engine.db.h2db;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.engine.db.DbServer;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The internal implementation of the db server.
 */
public final class H2dbServer implements DbServer {
    private final static Logger logger = Logger.getLogger(H2dbServer.class.getSimpleName());
    private static Server server = null;

    /**
     * Constructs a default db server.
     *
     * @throws SQLException when a db server encounters a problem.
     */
    public H2dbServer() throws SQLException {
        if (server == null) {
            server = Server.createTcpServer("-tcp", "-tcpPort", "8043");
        } else {
            throw new RuntimeException("Server has been already created");
        }
    }

    /**
     * Constructs a db server based on the configuration.
     *
     * @param config config a configuration of a db server.
     * @throws SQLException when a db server encounters a problem.
     */
    public H2dbServer(final Config config) throws SQLException {
        throw new RuntimeException("constructor with the configuration is not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DbServer start() throws SQLException {
        if (!server.isRunning(true)) {
            server.start();
            logger.log(Level.INFO, "Server started");
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (server.isRunning(true)) {
            server.stop();
            logger.log(Level.INFO, "Server stopped");
        }
    }
}
