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

import static com.github.akarazhev.metaconfig.Constants.Messages.IMPLEMENTATION_NOT_PROVIDED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;

/**
 * The internal implementation of the db server.
 */
public final class H2dbServer implements DbServer {
    private final static Logger logger = Logger.getLogger(H2dbServer.class.getSimpleName());
    private Server dbServer;

    /**
     * Constructs a default db server.
     *
     * @throws SQLException when a db server encounters a problem.
     */
    public H2dbServer() throws SQLException {
        dbServer = Server.createTcpServer("-tcp", "-tcpPort", "8043"); }

    /**
     * Constructs a db server based on the configuration.
     *
     * @param config config a configuration of a db server.
     * @throws SQLException when a db server encounters a problem.
     */
    public H2dbServer(final Config config) throws SQLException {
        throw new RuntimeException(IMPLEMENTATION_NOT_PROVIDED); // TODO: implement it
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DbServer start() throws SQLException {
        if (!dbServer.isRunning(true)) {
            dbServer.start();
            logger.log(Level.INFO, SERVER_STARTED);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (dbServer.isRunning(true)) {
            dbServer.stop();
            logger.log(Level.INFO, SERVER_STOPPED);
        }
    }
}
