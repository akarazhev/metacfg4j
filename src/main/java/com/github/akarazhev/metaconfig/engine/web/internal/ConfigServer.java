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
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.Messages.IMPLEMENTATION_NOT_PROVIDED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_ALREADY_CREATED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIGS;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG_NAMES;

/**
 * The internal implementation of the web server.
 */
public final class ConfigServer implements WebServer {
    private final static Logger logger = Logger.getLogger(ConfigServer.class.getSimpleName());
    private static HttpServer server = null;

    /**
     * Constructs a default web server.
     *
     * @param configService a configuration service.
     * @throws IOException when a web server encounters a problem.
     */
    public ConfigServer(final ConfigService configService) throws IOException {
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext(ACCEPT_CONFIG, new AcceptConfigController.Builder(configService).build()::handle);
            server.createContext(CONFIG_NAMES, new ConfigNamesController.Builder(configService).build()::handle);
            server.createContext(CONFIGS, new ConfigsController.Builder(configService).build()::handle);
            server.createContext(CONFIG, new ConfigController.Builder(configService).build()::handle);
            server.setExecutor(null);
        } else {
            throw new RuntimeException(SERVER_ALREADY_CREATED);
        }
    }

    /**
     * Constructs a web server based on the configuration.
     *
     * @param config config a configuration of a web server.
     * @param configService a configuration service.
     * @throws IOException when a web server encounters a problem.
     */
    public ConfigServer(final Config config, final ConfigService configService) throws IOException {
        throw new RuntimeException(IMPLEMENTATION_NOT_PROVIDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebServer start() {
        server.start();
        logger.log(Level.INFO, SERVER_STARTED);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        server.stop(0);
        logger.log(Level.INFO, SERVER_STOPPED);
    }
}
