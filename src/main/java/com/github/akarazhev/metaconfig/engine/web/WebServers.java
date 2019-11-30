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
import com.github.akarazhev.metaconfig.engine.web.server.Server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Provides factory methods to create a web server.
 */
public final class WebServers {

    private WebServers() {
        throw new AssertionError("Factory class ca not be ");
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

            @Override
            public Stream<Config> update(final Stream<Config> stream) {
                final Config[] input = stream.toArray(Config[]::new);
                final Config[] output = new Config[input.length];
                for (int i = 0; i < input.length; i++) {
                    if (input[i].getId() == 0) {
                        input[i] = new Config.Builder(input[i]).id(1).build();
                    }

                    output[i] = input[i];
                    dataStorage.put(output[i].getName(), output[i]);
                }

                return Arrays.stream(output);
            }

            @Override
            public Stream<String> getNames() {
                return dataStorage.keySet().stream().sorted();
            }

            @Override
            public Stream<Config> get() {
                return dataStorage.values().stream().sorted(Comparator.comparing(Config::getName));
            }

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

            @Override
            public int remove(final Stream<String> stream) {
                int size = dataStorage.size();
                stream.forEach(dataStorage::remove);
                return size - dataStorage.size();
            }

            @Override
            public void accept(final String name) {
                if (consumer != null) {
                    get(Stream.of(name)).findAny().ifPresent(config -> consumer.accept(config));
                }
            }

            @Override
            public void addConsumer(final Consumer<Config> consumer) {
                this.consumer = consumer;
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
