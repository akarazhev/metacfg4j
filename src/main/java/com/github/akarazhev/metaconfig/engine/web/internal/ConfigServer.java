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
package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG_NAMES;

/**
 * The internal implementation of the web server.
 */
public final class ConfigServer implements WebServer {
    private final static Logger LOGGER = Logger.getLogger(ConfigServer.class.getSimpleName());
    private HttpServer httpServer;
    /**
     * Settings constants for the web server.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        public static final String CONFIG_NAME = "config-server";
        // The port key
        public static final String PORT = "port";
        // The backlog key
        public static final String BACKLOG = "backlog";
    }
    /**
     * Constructs a default web server.
     *
     * @param configService a configuration service.
     * @throws IOException when a web server encounters a problem.
     */
    public ConfigServer(final ConfigService configService) throws IOException {
        // Set the default config
        this(new Config.Builder(Settings.CONFIG_NAME, Arrays.asList(
                new Property.Builder(Settings.PORT, 8000).build(),
                new Property.Builder(Settings.BACKLOG, 0).build())).build(), configService);
    }

    /**
     * Constructs a web server based on the configuration.
     *
     * @param config config a configuration of a web server.
     * @param configService a configuration service.
     * @throws IOException when a web server encounters a problem.
     */
    public ConfigServer(final Config config, final ConfigService configService) throws IOException {
        // Validate the config
        final Config serverConfig = Validator.of(config).
                validate(c -> Settings.CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(Settings.PORT).isPresent(), "Port is not presented.").
                validate(c -> c.getProperty(Settings.BACKLOG).isPresent(), "Backlog is not presented.").
                get();
        // Get the port
        final int port = serverConfig.getProperty(Settings.PORT).
                map(property -> (int) property.asLong()).
                orElse(8000);
        // Get the backlog
        final int backlog = serverConfig.getProperty(Settings.BACKLOG).
                map(property -> (int) property.asLong()).
                orElse(0);
        // Init the server
        httpServer = HttpServer.create(new InetSocketAddress(port), backlog);
        httpServer.createContext(ACCEPT_CONFIG, new AcceptConfigController.Builder(configService).build()::handle);
        httpServer.createContext(CONFIG_NAMES, new ConfigNamesController.Builder(configService).build()::handle);
        httpServer.createContext(CONFIG, new ConfigController.Builder(configService).build()::handle);
        httpServer.setExecutor(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebServer start() {
        httpServer.start();
        LOGGER.log(Level.INFO, SERVER_STARTED);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        httpServer.stop(0);
        LOGGER.log(Level.INFO, SERVER_STOPPED);
    }
}
