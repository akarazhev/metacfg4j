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
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.engine.db.DbServer;
import com.github.akarazhev.metaconfig.extension.Validator;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;

/**
 * The internal implementation of the h2db server.
 */
public final class H2dbServer implements DbServer {
    private final static Logger LOGGER = Logger.getLogger(H2dbServer.class.getSimpleName());
    private Server dbServer;
    /**
     * Settings constants for the h2db server.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        public static final String CONFIG_NAME = "db-server";
        // The type key
        public static final String TYPE = "type";
        // The type tcp value
        public static final String TYPE_TCP = "tcp";
        // The type pg value
        public static final String TYPE_PG = "pg";
        // The type web value
        public static final String TYPE_WEB = "web";
        // The args key
        public static final String ARGS = "args";
        // The args value
        static final String[] ARGS_VALUE = new String[]{"-tcp", "-tcpPort", "8043"};
    }

    /**
     * Constructs a default h2db server.
     *
     * @throws SQLException when a h2db server encounters a problem.
     */
    public H2dbServer() throws SQLException {
        this(new Config.Builder(Settings.CONFIG_NAME, Arrays.asList(
                new Property.Builder(Settings.TYPE, Settings.TYPE_TCP).build(),
                new Property.Builder(Settings.ARGS, Settings.ARGS_VALUE).build())).build());
    }

    /**
     * Constructs a h2db server based on the configuration.
     *
     * @param config config a configuration of a h2db server.
     * @throws SQLException when a h2db server encounters a problem.
     */
    public H2dbServer(final Config config) throws SQLException {
        // Validate the config
        final Config h2DbConfig = Validator.of(config).
                validate(c -> Settings.CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(Settings.TYPE).isPresent(), "Type is not present.").
                validate(c -> c.getProperty(Settings.ARGS).isPresent(), "Args is not present.").
                get();
        // Get the args
        final String[] args = h2DbConfig.getProperty(Settings.ARGS).
                map(Property::asArray).
                orElse(new String[0]);
        // Get the type
        final String type = h2DbConfig.getProperty(Settings.TYPE).
                map(Property::getValue).
                orElse(Settings.TYPE_TCP);
        switch (type) {
            case Settings.TYPE_WEB:
                dbServer = Server.createWebServer(args);
                break;
            case Settings.TYPE_PG:
                dbServer = Server.createPgServer(args);
                break;
            case Settings.TYPE_TCP:
                dbServer = Server.createTcpServer(args);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DbServer start() throws SQLException {
        if (!dbServer.isRunning(true)) {
            dbServer.start();
            LOGGER.log(Level.INFO, SERVER_STARTED);
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
            LOGGER.log(Level.INFO, SERVER_STOPPED);
        }
    }
}
