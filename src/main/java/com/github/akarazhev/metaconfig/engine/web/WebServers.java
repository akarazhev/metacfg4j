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
package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.api.PageRequest;
import com.github.akarazhev.metaconfig.api.PageResponse;
import com.github.akarazhev.metaconfig.engine.web.server.Server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_FACTORY_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ERROR;

/**
 * Provides factory methods to create a web server.
 */
public final class WebServers {

    private WebServers() {
        throw new AssertionError(CREATE_FACTORY_CLASS_ERROR);
    }

    /**
     * Returns a test web server.
     *
     * @return a test web server.
     * @throws Exception when a test web server encounters a problem.
     */
    public static WebServer newTestServer() throws Exception {
        return newServer(new ConfigService() {
            private Consumer<Config> consumer;
            private final Map<String, Config> dataStorage = new ConcurrentHashMap<>();

            /**
             * {@inheritDoc}
             */
            @Override
            public Stream<Config> update(final Stream<Config> stream) {
                final Config[] input = stream.toArray(Config[]::new);
                final Config[] output = new Config[input.length];
                for (int i = 0; i < input.length; i++) {
                    final Config config = dataStorage.get(input[i].getName());
                    if (config != null) {
                        if (config.getVersion() == input[i].getVersion()) {
                            input[i] = new Config.Builder(input[i]).version(input[i].getVersion() + 1).build();
                        } else {
                            throw new RuntimeException(DB_ERROR);
                        }
                    } else {
                        if (input[i].getId() == 0) {
                            input[i] = new Config.Builder(input[i]).id(1).build();
                        }
                    }

                    output[i] = input[i];
                    dataStorage.put(output[i].getName(), output[i]);
                }

                return Arrays.stream(output);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Stream<String> getNames() {
                return dataStorage.keySet().stream().sorted();
            }

            @Override
            public PageResponse getNames(final PageRequest request) {
                final List<String> data = dataStorage.values().stream().
                        filter(config -> {
                            if (config.getName().toLowerCase().contains(request.getName().toLowerCase())) {
                                if (config.getAttributes().isPresent()) {
                                    return contains(request.getAttributes(), config.getAttributes().get());
                                }

                                return true;
                            } else {
                                return false;
                            }
                        }).
                        map(Config::getName).
                        sorted(request.isAscending() ? String::compareTo : Collections.reverseOrder()).
                        collect(Collectors.toList());
                final int size = Math.min(request.getSize(), data.size());
                final Collection<String> names = data.size() > 0 ?
                        data.subList(request.getPage() * size, request.getPage() * size + size) :
                        Collections.emptyList();

                return new PageResponse.Builder(names).
                        page(request.getPage()).
                        total(data.size()).
                        build();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Stream<Config> get() {
                return dataStorage.values().stream().sorted(Comparator.comparing(Config::getName));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Stream<Config> get(final Stream<String> stream) {
                final Collection<Config> configs = new LinkedList<>();
                stream.forEach(name -> {
                    final Config config = dataStorage.get(name);
                    if (config != null) {
                        configs.add(config);
                    }
                });

                return configs.stream().sorted(Comparator.comparing(Config::getName));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int remove(final Stream<String> stream) {
                final int size = dataStorage.size();
                stream.forEach(dataStorage::remove);
                return size - dataStorage.size();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void accept(final Stream<String> stream) {
                if (consumer != null) {
                    get(stream).findAny().ifPresent(config -> consumer.accept(config));
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void addConsumer(final Consumer<Config> consumer) {
                this.consumer = consumer;
            }

            private boolean contains(final Map<String, String> filter, final Map<String, String> data) {
                if (filter.size() > 0) {
                    for (final String dataKey : data.keySet()) {
                        final String dataValue = data.get(dataKey);
                        for (final String filterKey : filter.keySet()) {
                            final String filterValue = filter.get(filterKey);
                            if (dataKey.toLowerCase().contains(filterKey.toLowerCase()) &&
                                    dataValue.toLowerCase().contains(filterValue.toLowerCase())) {
                                return true;
                            }
                        }
                    }

                    return false;
                }

                return true;
            }
        });
    }

    /**
     * Returns a default web server.
     *
     * @param configService a configuration service.
     * @return a web server.
     * @throws Exception when a web server encounters a problem.
     */
    public static WebServer newServer(final ConfigService configService) throws Exception {
        return new Server(configService);
    }

    /**
     * Returns a web server based on the configuration.
     *
     * @param config        config a configuration of a web server.
     * @param configService a configuration service.
     * @return a web server.
     * @throws Exception when a web server encounters a problem.
     */
    public static WebServer newServer(final Config config, final ConfigService configService) throws Exception {
        return new Server(config, configService);
    }
}
