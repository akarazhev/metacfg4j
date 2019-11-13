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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRepositoryTest {
    private static final String FIRST_CONFIG = "The First Config";

    private static ConnectionPool connectionPool;
    private static ConfigRepository configRepository;
    private static DbServer dbServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        if (dbServer == null) {
            dbServer = DbServers.newServer().start();
        }

        if (connectionPool == null) {
            connectionPool = ConnectionPools.newPool();
        }

        if (configRepository == null) {
            configRepository = new ConfigRepositoryImpl.Builder(connectionPool.getDataSource()).build();
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }

        if (dbServer != null) {
            dbServer.stop();
            dbServer = null;
        }

//        Files.deleteIfExists(Paths.get("data"));
    }

    @BeforeEach
    void beforeEach() {
        final Property firstSubProperty = new Property.Builder("Sub-Property-1", "Sub-Value-1").build();
        final Property secondSubProperty = new Property.Builder("Sub-Property-2", "Sub-Value-2").build();
        final Property thirdSubProperty = new Property.Builder("Sub-Property-3", "Sub-Value-3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attributes(Collections.singletonMap("key", "value")).
                property(new String[0], firstSubProperty).
                property(new String[]{"Sub-Property-1"}, secondSubProperty).
                property(new String[]{"Sub-Property-1", "Sub-Property-2"}, thirdSubProperty).
                build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        attributes.put("key_3", "value_3");

        configRepository.saveAndFlush(Stream.of(
                new Config.Builder(FIRST_CONFIG, Collections.singletonList(property)).attributes(attributes).build()));
    }

    @AfterEach
    void afterEach() {
        configRepository.delete(Stream.of(FIRST_CONFIG));
    }

    @Test
    void findConfigByName() {
        final Optional<Config> config = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(config.isPresent());
        assertEquals(FIRST_CONFIG, config.get().getName());
        assertTrue(config.get().getAttributes().isPresent());
        assertEquals(3, config.get().getAttributes().get().size());
    }
}
