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
package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.engine.db.DbServer;
import com.github.akarazhev.metaconfig.engine.db.DbServers;
import com.github.akarazhev.metaconfig.engine.db.pool.ConnectionPool;
import com.github.akarazhev.metaconfig.engine.db.pool.ConnectionPools;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.engine.web.WebServers;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.META_CONFIG_ERROR;

/**
 * The core configuration class that provides the functionality.
 */
public final class MetaConfig implements ConfigService, Closeable {
    private final DbServer dbServer;
    private final WebServer webServer;
    private final ConnectionPool connectionPool;
    private final ConfigService configService;

    private MetaConfig(final DbServer dbServer, final WebServer webServer,
                       final ConnectionPool connectionPool, final ConfigService configService) {
        this.dbServer = dbServer;
        this.webServer = webServer;
        this.connectionPool = connectionPool;
        this.configService = configService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> update(final Stream<Config> stream) {
        return configService.update(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getNames() {
        return configService.getNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> get() {
        return configService.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> get(final Stream<String> stream) {
        return configService.get(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remove(final Stream<String> stream) {
        return configService.remove(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(final String name) {
        configService.accept(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addConsumer(final Consumer<Config> consumer) {
        configService.addConsumer(consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // Stop the web server
        if (webServer != null) {
            webServer.stop();
        }
        // Close the connection pool
        if (connectionPool != null) {
            connectionPool.close();
        }
        // Stop the database server
        if (dbServer != null) {
            dbServer.stop();
        }
    }

    /**
     * Wraps and builds the instance of the core configuration class.
     */
    public final static class Builder {
        private Config dbConfig;
        private Config webConfig;
        private Config poolConfig;

        /**
         * The default constructor.
         */
        public Builder() {
        }

        /**
         * Constructs the core configuration class with the configuration of a db server.
         *
         * @param config a configuration of a db server.
         * @return a builder of the core configuration class.
         */
        public Builder dbServer(final Config config) {
            this.dbConfig = config;
            return this;
        }

        /**
         * Constructs the core configuration class with the configuration of a web server.
         *
         * @param config a configuration a web server.
         * @return a builder of the core configuration class.
         */
        public Builder webServer(final Config config) {
            this.webConfig = config;
            return this;
        }

        /**
         * Constructs the core configuration class with the configuration of a connection pool.
         *
         * @param config a configuration a connection pool.
         * @return a builder of the core configuration class.
         */
        public Builder connectionPool(final Config config) {
            this.poolConfig = config;
            return this;
        }

        /**
         * Builds the core configuration class with parameters.
         *
         * @return a builder of the core configuration class.
         */
        public MetaConfig build() {
            DbServer dbServer;
            ConnectionPool connectionPool;
            WebServer webServer;
            ConfigService configService;
            try {
                // DB Server
                dbServer = dbConfig == null ? DbServers.newServer().start() : DbServers.newServer(dbConfig).start();
                // Connection pool
                connectionPool = poolConfig == null ? ConnectionPools.newPool() : ConnectionPools.newPool(poolConfig);
                // Config Repository
                final ConfigRepository configRepository =
                        new ConfigRepositoryImpl.Builder(connectionPool.getDataSource()).build();
                // Config service
                configService = new ConfigServiceImpl.Builder(configRepository).build();
                // Web server
                if (webConfig != null) {
                    webServer = WebServers.newServer(webConfig, configService).start();
                } else {
                    webServer = WebServers.newServer(configService).start();
                }
                // Create the main instance
                return new MetaConfig(dbServer, webServer, connectionPool, configService);
            } catch (final Exception e) {
                throw new RuntimeException(META_CONFIG_ERROR, e);
            }
        }
    }
}
