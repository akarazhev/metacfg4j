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
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.engine.web.server.ConfigServer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
            private final Map<String, Config> map = new HashMap<String, Config>() {{
                put("name", new Config.Builder("name", Collections.singletonList(
                        new Property.Builder("name", "value").build())).build());
            }};

            @Override
            public Stream<Config> update(final Stream<Config> stream) {
                final Collection<Config> list = stream.collect(Collectors.toList());
                for (final Config config : list) {
                    map.put(config.getName(), config);
                }

                return list.stream();
            }

            @Override
            public Stream<String> getNames() {
                return map.keySet().stream();
            }

            @Override
            public Stream<Config> get() {
                return map.values().stream();
            }

            @Override
            public Stream<Config> get(final Stream<String> stream) {
                final Collection<Config> configs = new LinkedList<>();
                stream.forEach(name -> configs.add(map.get(name)));
                return configs.stream();
            }

            @Override
            public int remove(final Stream<String> stream) {
                int size = map.size();
                stream.forEach(map::remove);
                return size - map.size();
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
        return new ConfigServer(configService);
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
        return new ConfigServer(config, configService);
    }
}
