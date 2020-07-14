/* Copyright 2019-2020 Andrey Karazhev
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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_NOT_PRESENTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.ARGS;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.ARGS_VALUE;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.TYPE;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.TYPE_PG;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.TYPE_TCP;
import static com.github.akarazhev.metaconfig.engine.db.h2db.Server.Settings.TYPE_WEB;

/**
 * The internal implementation of the h2db server.
 */
public final class Server implements DbServer {
    private final static Logger LOGGER = Logger.getLogger(Server.class.getSimpleName());
    private org.h2.tools.Server dbServer;
    /**
     * Settings constants for the h2db server.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        static final String CONFIG_NAME = "db-server";
        // The type key
        static final String TYPE = "type";
        // The type tcp value
        static final String TYPE_TCP = "tcp";
        // The type pg value
        static final String TYPE_PG = "pg";
        // The type web value
        static final String TYPE_WEB = "web";
        // The args key
        static final String ARGS = "args";
        // The args value
        static final String[] ARGS_VALUE = new String[]{"-tcp", "-tcpPort", "8043"};
    }

    /**
     * Constructs a default h2db server.
     *
     * @throws SQLException when a h2db server encounters a problem.
     */
    public Server() throws SQLException {
        this(new Config.Builder(CONFIG_NAME, Arrays.asList(new Property.Builder(TYPE, TYPE_TCP).build(),
                new Property.Builder(ARGS, ARGS_VALUE).build())).build());
    }

    /**
     * Constructs a h2db server based on the configuration.
     *
     * @param config config a configuration of a h2db server.
     * @throws SQLException when a h2db server encounters a problem.
     */
    public Server(final Config config) throws SQLException {
        // Validate the config
        final Config h2DbConfig = Validator.of(config).
                validate(c -> CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(TYPE).isPresent(), String.format(PARAM_NOT_PRESENTED, TYPE)).
                validate(c -> c.getProperty(ARGS).isPresent(), String.format(PARAM_NOT_PRESENTED, ARGS)).
                get();
        // Get the args
        final String[] args = h2DbConfig.getProperty(ARGS).
                map(Property::asArray).
                orElse(new String[0]);
        // Get the type
        final String type = h2DbConfig.getProperty(TYPE).
                map(Property::getValue).
                orElse(TYPE_TCP);
        switch (type) {
            case TYPE_WEB:
                dbServer = org.h2.tools.Server.createWebServer(args);
                break;
            case TYPE_PG:
                dbServer = org.h2.tools.Server.createPgServer(args);
                break;
            case TYPE_TCP:
                dbServer = org.h2.tools.Server.createTcpServer(args);
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
