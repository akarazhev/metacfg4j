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
package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.engine.web.WebServers;
import com.github.akarazhev.metaconfig.extension.Validator;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.META_CONFIG_ERROR;

/**
 * The core configuration class that provides the functionality.
 */
public final class MetaConfig implements ConfigService, Closeable {
    private final WebServer webServer;
    private final ConfigService configService;

    private MetaConfig(final WebServer webServer, final ConfigService configService) {
        this.webServer = webServer;
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
    public PageResponse getNames(final PageRequest request) {
        return configService.getNames(request);
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
    public void accept(final Stream<String> stream) {
        configService.accept(stream);
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
    public void close() {
        // Stop the web server
        if (webServer != null) {
            webServer.stop();
        }
    }

    /**
     * Wraps and builds the instance of the core configuration class.
     */
    public final static class Builder {
        private Config webClient;
        private Config webConfig;
        private Map<String, String> dataMapping;
        private DataSource dataSource;
        private boolean isDefaultConfig;

        /**
         * The default constructor.
         */
        public Builder() {
            this.isDefaultConfig = false;
        }

        /**
         * Constructs the core configuration class with the configuration of a web client.
         *
         * @param config a configuration a web client.
         * @return a builder of the core configuration class.
         */
        public Builder webClient(final Config config) {
            this.webClient = Validator.of(config).get();
            return this;
        }

        /**
         * Constructs the core configuration class with the configuration of a web server.
         *
         * @param config a configuration a web server.
         * @return a builder of the core configuration class.
         */
        public Builder webServer(final Config config) {
            this.webConfig = Validator.of(config).get();
            return this;
        }

        /**
         * Constructs the core configuration class with the custom mapping.
         *
         * @param mapping a table mapping.
         * @return a builder of the core configuration class.
         */
        public Builder dataMapping(final Map<String, String> mapping) {
            this.dataMapping = Validator.of(mapping).get();
            return this;
        }

        /**
         * Constructs the core configuration class with an existed data source.
         *
         * @param dataSource a data source.
         * @return a builder of the core configuration class.
         */
        public Builder dataSource(final DataSource dataSource) {
            this.dataSource = Validator.of(dataSource).get();
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
                // init a mapping
                final Map<String, String> mapping = dataMapping != null ? dataMapping : new HashMap<>();
                // Init the repository
                final var configRepository = dataSource != null ?
                        new DbConfigRepository.Builder(dataSource).mapping(mapping).build() :
                        new WebConfigRepository.Builder(webClient).build();
                // Init the config service
                final var configService = new ConfigServiceImpl.Builder(configRepository).build();
                // Init the web server
                WebServer webServer = null;
                if (isDefaultConfig) {
                    webServer = WebServers.newServer(configService).start();
                } else if (webConfig != null) {
                    webServer = WebServers.newServer(webConfig, configService).start();
                }
                // Create the main instance
                return new MetaConfig(webServer, configService);
            } catch (final Exception e) {
                throw new RuntimeException(META_CONFIG_ERROR, e);
            }
        }
    }
}
