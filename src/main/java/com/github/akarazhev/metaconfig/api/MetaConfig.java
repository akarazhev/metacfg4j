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

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
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

    private MetaConfig(final DbServer dbServer, final WebServer webServer, final ConnectionPool connectionPool,
                       final ConfigService configService) {
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
        private DataSource dataSource;
        private Config poolConfig;
        private boolean isDefaultConfig;

        /**
         * The default constructor.
         */
        public Builder() {
            this.isDefaultConfig = false;
        }

        /**
         * Constructs the core configuration class with the configuration of a db server.
         *
         * @param config a configuration of a db server.
         * @return a builder of the core configuration class.
         */
        public Builder dbServer(final Config config) {
            this.dbConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Constructs the core configuration class with the configuration of a web server.
         *
         * @param config a configuration a web server.
         * @return a builder of the core configuration class.
         */
        public Builder webServer(final Config config) {
            this.webConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Constructs the core configuration class with the configuration of a connection pool.
         *
         * @param config a configuration a connection pool.
         * @return a builder of the core configuration class.
         */
        public Builder connectionPool(final Config config) {
            this.poolConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Constructs the core configuration class with an existed data source.
         *
         * @param dataSource a data source.
         * @return a builder of the core configuration class.
         */
        public Builder dataSource(final DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource);
            return this;
        }

        /**
         * Constructs the core configuration with the default configuration.
         *
         * @return a builder of the core configuration class.
         */
        public Builder defaultConfig() {
            this.isDefaultConfig = true;
            return this;
        }

        /**
         * Builds the core configuration class with parameters.
         *
         * @return a builder of the core configuration class.
         */
        public MetaConfig build() {
            try {
                DbServer dbServer = null;
                ConnectionPool connectionPool = null;
                WebServer webServer = null;
                // Init the DB server
                if (isDefaultConfig) {
                    dbServer = DbServers.newServer().start();
                } else if (dbConfig != null) {
                    dbServer = DbServers.newServer(dbConfig).start();
                }
                // Init the connection pool and the datasource
                if (isDefaultConfig) {
                    connectionPool = ConnectionPools.newPool();
                    dataSource = connectionPool.getDataSource();
                } else if (dbConfig != null) {
                    connectionPool = ConnectionPools.newPool(poolConfig);
                    dataSource = connectionPool.getDataSource();
                }
                // Init the config service
                final ConfigService configService =
                        new ConfigServiceImpl.Builder(new DbConfigRepository.Builder(dataSource).build()).build();
                // Init the web server
                if (isDefaultConfig) {
                    webServer = WebServers.newServer(configService).start();
                } else if (webConfig != null) {
                    webServer = WebServers.newServer(webConfig, configService).start();
                }
                // Create the main instance
                return new MetaConfig(dbServer, webServer, connectionPool, configService);
            } catch (final Exception e) {
                throw new RuntimeException(META_CONFIG_ERROR, e);
            }
        }
    }
}
