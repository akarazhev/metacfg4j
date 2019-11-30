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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Data base repository test")
final class DbConfigRepositoryTest {
    private static final String FIRST_CONFIG = "The First Config";
    private static final String SECOND_CONFIG = "The Second Config";
    private static final String NEW_CONFIG = "New Config";

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
            configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
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

        configRepository = null;
    }

    @BeforeEach
    void beforeEach() {
        configRepository.saveAndFlush(Stream.of(getConfigWithSubProperties(FIRST_CONFIG),
                getConfigWithProperties(SECOND_CONFIG)));
    }

    @AfterEach
    void afterEach() {
        configRepository.delete(Stream.of(FIRST_CONFIG, SECOND_CONFIG, NEW_CONFIG));
    }

    @Test
    @DisplayName("Find configs by empty names")
    void findByEmptyNames() {
        // Check test results
        assertEquals(0, configRepository.findByNames(Stream.empty()).count());
    }

    @Test
    @DisplayName("Find configs by the not existed name")
    void findByNotExistedName() {
        // Check test results
        assertEquals(0, configRepository.findByNames(Stream.of(NEW_CONFIG)).count());
    }

    @Test
    @DisplayName("Find configs by names")
    void findConfigsByNames() {
        final Config[] configs =
                configRepository.findByNames(Stream.of(FIRST_CONFIG, SECOND_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(2, configs.length);
        final Config firstExpected = getConfigWithSubProperties(FIRST_CONFIG);
        final Config secondExpected = getConfigWithSubProperties(SECOND_CONFIG);
        assertEqualsConfig(firstExpected, configs[0]);
        assertEqualsProperty(firstExpected, configs[0]);
        assertEqualsConfig(secondExpected, configs[1]);
        assertEqualsProperty(secondExpected, configs[1]);
    }

    @Test
    @DisplayName("Find configs by names with the closed connection pool")
    void findByNamesWithClosedConnectionPool() throws IOException {
        connectionPool.close();
        // Check test results
        assertThrows(RuntimeException.class, () -> configRepository.findByNames(Stream.of(FIRST_CONFIG, SECOND_CONFIG)));
        connectionPool = ConnectionPools.newPool();
        configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
    }

    @Test
    @DisplayName("Find config names")
    void findNames() {
        final String[] names = configRepository.findNames().toArray(String[]::new);
        // Check test results
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names with the closed connection pool")
    void findNamesWithClosedConnectionPool() throws IOException {
        connectionPool.close();
        // Check test results
        assertThrows(RuntimeException.class, () -> configRepository.findNames());
        connectionPool = ConnectionPools.newPool();
        configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
    }

    @Test
    @DisplayName("Save and flush a new config")
    void saveAndFlushNewConfig() {
        final Optional<Config> newConfig =
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Save and flush an empty")
    void saveAndFlushEmptyConfig() {
        final Stream<Config> configs = configRepository.saveAndFlush(Stream.empty());
        // Check test results
        assertEquals(0, configs.count());
    }

    @Test
    @DisplayName("Save and flush by the config id")
    void saveAndFlushConfigById() {
        final Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).
                id(firstConfig.get().getId()).
                build();
        Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(newConfig)).findFirst();
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Optimistic locking error")
    void optimisticLockingError() {
        final Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(firstConfig.get()).build();
        configRepository.saveAndFlush(Stream.of(newConfig));
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
    }

    private void assertEqualsConfig(final Config expected, final Config actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getVersion(), actual.getVersion());

        assertTrue(actual.getAttributes().isPresent());
        assertTrue(expected.getAttributes().isPresent());
        assertEquals(expected.getAttributes(), actual.getAttributes());
    }

    private void assertEqualsProperty(final Config expectedConfig, final Config actualConfig) {
        final Optional<Property> expectedProperty = expectedConfig.getProperty("Property");
        assertTrue(expectedProperty.isPresent());
        final Property expected = expectedProperty.get();
        final Optional<Property> actualProperty = actualConfig.getProperty("Property");
        assertTrue(actualProperty.isPresent());
        final Property actual = actualProperty.get();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getCaption(), actual.getCaption());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getValue(), actual.getValue());

        assertTrue(actual.getAttributes().isPresent());
        assertTrue(expected.getAttributes().isPresent());
        assertEquals(expected.getAttributes(), actual.getAttributes());
    }

    private Config getConfigWithSubProperties(final String name) {
        final Property firstSubProperty = new Property.Builder("Sub-Property-1", "Sub-Value-1").
                attribute("key_1", "value_1").build();
        final Property secondSubProperty = new Property.Builder("Sub-Property-2", "Sub-Value-2").
                attribute("key_2", "value_2").build();
        final Property thirdSubProperty = new Property.Builder("Sub-Property-3", "Sub-Value-3").
                attribute("key_3", "value_3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attribute("key", "value").
                property(new String[0], firstSubProperty).
                property(new String[]{"Sub-Property-1"}, secondSubProperty).
                property(new String[]{"Sub-Property-1", "Sub-Property-2"}, thirdSubProperty).
                build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        attributes.put("key_3", "value_3");

        return new Config.Builder(name, Collections.singletonList(property)).attributes(attributes).build();
    }

    private Config getConfigWithProperties(final String name) {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").
                attribute("key_1", "value_1").build();
        final Property secondProperty = new Property.Builder("Property-2", "Value-2").
                attribute("key_2", "value_2").build();
        final Property thirdProperty = new Property.Builder("Property-3", "Value-3").
                attribute("key_3", "value_3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attribute("key", "value").
                property(new String[0], firstProperty).
                property(new String[0], secondProperty).
                property(new String[0], thirdProperty).
                build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        attributes.put("key_3", "value_3");

        return new Config.Builder(name, Collections.singletonList(property)).attributes(attributes).build();
    }
}
