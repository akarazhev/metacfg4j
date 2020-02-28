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

import com.github.akarazhev.metaconfig.UnitTest;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Data base repository test")
final class DbConfigRepositoryTest extends UnitTest {
    private static DbServer dbServer;
    private static ConnectionPool connectionPool;
    private static ConfigRepository configRepository;

    @BeforeAll
    static void beforeAll() throws Exception {
        if (dbServer == null) {
            dbServer = DbServers.newServer().start();
        }

        if (connectionPool == null) {
            connectionPool = ConnectionPools.newPool();
        }

        if (configRepository == null) {
            createRepository();
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        configRepository = null;

        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }

        if (dbServer != null) {
            dbServer.stop();
            dbServer = null;
        }
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
    @DisplayName("Find configs by names with not existed tables")
    void findByNamesWithNotExistedTables() throws SQLException {
        dropTables();
        // Check test results
        assertThrows(RuntimeException.class, () -> configRepository.findByNames(Stream.of(FIRST_CONFIG, SECOND_CONFIG)));
        createRepository();
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
    @DisplayName("Find config names with not existed tables")
    void findNamesWithNotExistedTables() throws SQLException {
        dropTables();
        // Check test results
        assertThrows(RuntimeException.class, () -> configRepository.findNames());
        createRepository();
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
    @DisplayName("Save and flush a new config with properties")
    void saveAndFlushNewConfigWithProperties() {
        final Optional<Config> newConfig =
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and read a large config with properties")
    void saveAndReadLargeConfigWithProperties() {
        // Save a large config
        System.out.println("Start saveAndFlush");
        long time = System.currentTimeMillis();
        Optional<Config> config = configRepository.saveAndFlush(Stream.of(getLargeConfig(100))).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        assertTrue(config.isPresent());
        // Read a large config
        System.out.println("Start findByNames");
        time = System.currentTimeMillis();
        config = configRepository.findByNames(Stream.of(config.get().getName())).findFirst();
        System.out.println("End findByNames in " + (System.currentTimeMillis() - time) + " ms.");
        assertTrue(config.isPresent());
    }

    @Test
    @DisplayName("Save and flush an updated large config with properties")
    void saveAndFlushUpdatedLargeConfigWithProperties() {
        // Save a large config
        System.out.println("Start saveAndFlush");
        long time = System.currentTimeMillis();
        final Optional<Config> largeConfig = configRepository.saveAndFlush(Stream.of(getLargeConfig(100))).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        // Check test results
        assertTrue(largeConfig.isPresent());
        // Update a large config
        final Config config = new Config.Builder(largeConfig.get()).
                properties(getProperties(1, 50)).
                build();
        time = System.currentTimeMillis();
        System.out.println("Start saveAndFlush");
        final Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        assertTrue(updatedConfig.isPresent());
    }

    @Test
    @DisplayName("Save and flush a new config with sub properties")
    void saveAndFlushNewConfigWithSubProperties() {
        final Optional<Config> newConfig =
                configRepository.saveAndFlush(Stream.of(getConfigWithSubProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated new config with property")
    void saveAndFlushUpdatedNewConfigWithProperty() {
        final Config config = new Config.Builder(NEW_CONFIG, Collections.singletonList(getProperty())).
                attribute("key_1", "value_1").
                attribute("key_2", "value_2").
                attribute("key_3", "value_3").build();
        final Optional<Config> newConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));

        assertTrue(newConfig.get().getProperty("Property-1").isPresent());
        final Property firstProperty = new Property.Builder("Value", 1000).
                id(newConfig.get().getProperty("Property-1").get().getId()).
                caption("Caption").
                attribute("key_1", "value-1").
                attribute("key_4", "value_4").
                description("Description").build();
        final Config updateConfig = new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).
                id(newConfig.get().getId()).
                description("Description").
                attribute("key_1", "value-1").
                attribute("key_4", "value_4").build();
        final Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(updateConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush with not the existed config attributes table")
    void saveAndFlushWithNotExistedConfigAttributesTable() throws SQLException {
        dropConfigAttributesTables();
        // Check test results
        assertThrows(RuntimeException.class, () ->
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))));
        createRepository();
    }

    @Test
    @DisplayName("Save and flush with not the existed property attributes table")
    void saveAndFlushWithNotExistedConfigPropertiesTable() throws SQLException {
        dropPropertyAttributesTables();
        // Check test results
        assertThrows(RuntimeException.class, () ->
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))));
        createRepository();
    }

    @Test
    @DisplayName("Save and flush with not existed tables")
    void saveAndFlushWithNotExistedTables() throws SQLException {
        dropTables();
        // Check test results
        assertThrows(RuntimeException.class, () ->
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))));
        createRepository();
    }

    @Test
    @DisplayName("Save and flush an empty")
    void saveAndFlushEmptyConfig() {
        // Check test results
        assertEquals(0, configRepository.saveAndFlush(Stream.empty()).count());
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
    @DisplayName("Save and flush by the config id with not the existed config attributes table")
    void saveAndFlushConfigByIdWithNotExistedConfigAttributesTable() throws SQLException {
        final Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(getConfigWithSubProperties(NEW_CONFIG)).
                id(firstConfig.get().getId()).
                build();
        dropConfigAttributesTables();
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
        createRepository();
    }

    @Test
    @DisplayName("Save and flush by the config id with not the existed property attributes table")
    void saveAndFlushConfigByIdWithNotExistedPropertyAttributesTable() throws SQLException {
        final Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(getConfigWithSubProperties(NEW_CONFIG)).
                id(firstConfig.get().getId()).
                build();
        dropConfigAttributesTables();
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
        createRepository();
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

    @Test
    @DisplayName("Delete configs by empty names")
    void deleteByEmptyNames() {
        // Check test results
        assertEquals(0, configRepository.delete(Stream.empty()));
    }

    @Test
    @DisplayName("Delete configs by the not existed name")
    void deleteByNotExistedName() {
        // Check test results
        assertEquals(0, configRepository.delete(Stream.of(NEW_CONFIG)));
    }

    @Test
    @DisplayName("Save and flush with not existed tables")
    void deleteByNotExistedTables() throws SQLException {
        dropTables();
        // Check test results
        assertThrows(RuntimeException.class, () -> configRepository.delete(Stream.of(NEW_CONFIG)));
        createRepository();
    }

    @Test
    @DisplayName("Delete configs by names")
    void deleteByNames() {
        // Check test results
        assertEquals(2, configRepository.delete(Stream.of(FIRST_CONFIG, SECOND_CONFIG)));
    }

    private static void createRepository() {
        final Map<String, String> mapping = new HashMap<>();
        mapping.put("configs", "CONFIGS");
        mapping.put("config-attributes", "CONFIG_ATTRIBUTES");
        mapping.put("properties", "PROPERTIES");
        mapping.put("property-attributes", "PROPERTY_ATTRIBUTES");

        configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).mapping(mapping).build();
    }

    private static void dropConfigAttributesTables() throws SQLException {
        try (final Connection connection = connectionPool.getDataSource().getConnection();
             final Statement statement = connection.createStatement()) {
            execute(statement, "CONFIG_ATTRIBUTES");
        }
    }

    private static void dropPropertyAttributesTables() throws SQLException {
        try (final Connection connection = connectionPool.getDataSource().getConnection();
             final Statement statement = connection.createStatement()) {
            execute(statement, "PROPERTY_ATTRIBUTES");
        }
    }

    private static void dropTables() throws SQLException {
        try (final Connection connection = connectionPool.getDataSource().getConnection();
             final Statement statement = connection.createStatement()) {
            execute(statement, "PROPERTY_ATTRIBUTES");
            execute(statement, "PROPERTIES");
            execute(statement, "CONFIG_ATTRIBUTES");
            execute(statement, "CONFIGS");
        }
    }

    private static void execute(final Statement statement, final String table) throws SQLException {
        statement.executeUpdate("DROP TABLE " + table + ";");
    }
}
