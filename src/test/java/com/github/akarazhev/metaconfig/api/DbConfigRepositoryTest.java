/* Copyright 2019-2021 Andrey Karazhev
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
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    private static void dropConfigAttributesTables() throws SQLException {
        try (final var connection = connectionPool.getDataSource().getConnection();
             final var statement = connection.createStatement()) {
            execute(statement, "CONFIG_ATTRIBUTES");
        }
    }

    private static void dropPropertyAttributesTables() throws SQLException {
        try (final var connection = connectionPool.getDataSource().getConnection();
             final var statement = connection.createStatement()) {
            execute(statement, "PROPERTY_ATTRIBUTES");
        }
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

    private static void dropTables() throws SQLException {
        try (final var connection = connectionPool.getDataSource().getConnection();
             final var statement = connection.createStatement()) {
            execute(statement, "PROPERTY_ATTRIBUTES");
            execute(statement, "PROPERTIES");
            execute(statement, "CONFIG_ATTRIBUTES");
            execute(statement, "CONFIGS");
        }
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
    @DisplayName("Find configs by names")
    void findConfigsByNames() {
        Config[] configs = configRepository.findByNames(Stream.of(FIRST_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(1, configs.length);
        final var firstExpected = getConfigWithSubProperties(FIRST_CONFIG);
        assertEqualsConfig(firstExpected, configs[0]);
        assertEqualsProperty(firstExpected, configs[0]);

        configs = configRepository.findByNames(Stream.of(SECOND_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(1, configs.length);
        final var secondExpected = getConfigWithSubProperties(SECOND_CONFIG);
        assertEqualsConfig(secondExpected, configs[0]);
        assertEqualsProperty(secondExpected, configs[0]);
    }

    @Test
    @DisplayName("Find all configs by names")
    void findAllConfigsByNames() {
        final var configs = configRepository.findByNames(Stream.of(FIRST_CONFIG, SECOND_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(2, configs.length);
        final var firstExpected = getConfigWithSubProperties(FIRST_CONFIG);
        final var secondExpected = getConfigWithSubProperties(SECOND_CONFIG);
        assertEqualsConfig(firstExpected, configs[0]);
        assertEqualsProperty(firstExpected, configs[0]);
        assertEqualsConfig(secondExpected, configs[1]);
        assertEqualsProperty(secondExpected, configs[1]);
    }

    @Test
    @DisplayName("Find config names")
    void findNames() {
        final var names = configRepository.findNames().toArray(String[]::new);
        // Check test results
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names by a page request")
    void findByPageRequest() {
        final var page = configRepository.findByPageRequest(new PageRequest.Builder(CONFIG).build());
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(2, page.getTotal());
        final var names = page.getNames().toArray(String[]::new);
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names by name with not existed tables")
    void findByNameWithNotExistedTables() throws SQLException {
        dropTables();
        // Check test results
        assertThrows(RuntimeException.class, () ->
                configRepository.findByPageRequest(new PageRequest.Builder(FIRST_CONFIG).build()));
        createRepository();
    }

    @Test
    @DisplayName("Find config names by name with the closed connection pool")
    void findByNameWithClosedConnectionPool() throws IOException {
        connectionPool.close();
        // Check test results
        assertThrows(RuntimeException.class, () ->
                configRepository.findByPageRequest(new PageRequest.Builder(FIRST_CONFIG).build()));
        connectionPool = ConnectionPools.newPool();
        configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
    }

    @Test
    @DisplayName("Find config names by a page request")
    void findByPageRequestAndAttributes() {
        final var request = new PageRequest.Builder(CONFIG).
                attributes(Collections.singletonMap("key", "value")).
                build();
        final var page = configRepository.findByPageRequest(request);
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(2, page.getTotal());
        final var names = page.getNames().toArray(String[]::new);
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names by a name, page, size and sorting")
    void findByNameAndPageAndSizeAndSorting() {
        final var request = new PageRequest.Builder(CONFIG).
                page(1).
                size(1).
                attribute("key", "value").
                ascending(false).
                build();
        final var page = configRepository.findByPageRequest(request);
        // Check test results
        assertEquals(1, page.getPage());
        assertEquals(2, page.getTotal());
        final var names = page.getNames().toArray(String[]::new);
        assertEquals(1, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
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
    @DisplayName("Find config names by a wrong name")
    void findByWrongName() {
        final var page = configRepository.findByPageRequest(new PageRequest.Builder(NEW_CONFIG).build());
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getNames().count());
    }

    @Test
    @DisplayName("Save and flush a new config with properties")
    void saveAndFlushNewConfigWithProperties() {
        final var newConfig =
                configRepository.saveAndFlush(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush a new config without properties")
    void saveAndFlushNewConfigWithoutProperties() {
        Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).build();
        configRepository.saveAndFlush(Stream.of(newConfig));
        final var updatedConfig = configRepository.findByNames(Stream.of(NEW_CONFIG)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        newConfig = updatedConfig.get();
        assertTrue(newConfig.getId() > 0);
        assertEquals(0, newConfig.getProperties().count());
    }

    @Test
    @DisplayName("Save and flush an updated large config with properties")
    void saveAndFlushUpdatedLargeConfigWithProperties() {
        // Save a large config
        System.out.println("Start saveAndFlush");
        long time = System.currentTimeMillis();
        final var largeConfig = configRepository.saveAndFlush(Stream.of(getLargeConfig(100))).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        sleep(1);
        // Check test results
        assertTrue(largeConfig.isPresent());
        // Update a large config
        final var config = new Config.Builder(largeConfig.get()).
                properties(getProperties(1, 50)).
                updated(Clock.systemDefaultZone().millis()).
                build();
        time = System.currentTimeMillis();
        System.out.println("Start saveAndFlush");
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        assertTrue(updatedConfig.isPresent());
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
    @DisplayName("Save and flush a new config with sub properties")
    void saveAndFlushNewConfigWithSubProperties() {
        final var newConfig =
                configRepository.saveAndFlush(Stream.of(getConfigWithSubProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated new config with property")
    void saveAndFlushUpdatedNewConfigWithProperty() {
        final var config = new Config.Builder(NEW_CONFIG, Collections.singletonList(getProperty())).
                attribute("key_1", "value_1").
                attribute("key_2", "value_2").
                attribute("key_3", "value_3").build();
        final var newConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        sleep(1);
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));

        assertTrue(newConfig.get().getProperty("Property-1").isPresent());
        final var firstProperty = new Property.Builder("Value", 1000).
                id(newConfig.get().getProperty("Property-1").get().getId()).
                caption("Caption").
                attribute("key_1", "value-1").
                attribute("key_4", "value_4").
                description("Description").build();
        final var updateConfig = new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).
                id(newConfig.get().getId()).
                description("Description").
                attribute("key_1", "value-1").
                attribute("key_4", "value_4").build();
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(updateConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated new config with property and empty attributes")
    void saveAndFlushUpdatedNewConfigWithPropertyAndEmptyAttributes() {
        final var config = new Config.Builder(NEW_CONFIG, Collections.singletonList(getProperty())).
                attribute("key_1", "value_1").
                attribute("key_2", "value_2").
                attribute("key_3", "value_3").build();
        final var newConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        sleep(1);
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));

        assertTrue(newConfig.get().getProperty("Property-1").isPresent());
        final var firstProperty = new Property.Builder("Value", 1000).
                id(newConfig.get().getProperty("Property-1").get().getId()).build();
        final var updateConfig = new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).
                id(newConfig.get().getId()).
                description("Description").build();
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(updateConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush a config without properties")
    void saveAndFlushConfigWithoutProperties() {
        Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        Config updatedConfig = new Config.Builder(firstConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                properties(Collections.emptyList()).
                build();
        configRepository.saveAndFlush(Stream.of(updatedConfig));
        firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        updatedConfig = firstConfig.get();
        assertTrue(updatedConfig.getId() > 0);
        assertEquals(0, updatedConfig.getProperties().count());
    }

    private static void createRepository() {
        final var mapping = new HashMap<String, String>();
        mapping.put("configs", "CONFIGS");
        mapping.put("config-attributes", "CONFIG_ATTRIBUTES");
        mapping.put("properties", "PROPERTIES");
        mapping.put("property-attributes", "PROPERTY_ATTRIBUTES");

        configRepository = new DbConfigRepository.Builder(connectionPool.getDataSource()).mapping(mapping).build();
    }

    @Test
    @DisplayName("Save and flush by the config id")
    void saveAndFlushConfigById() {
        final var firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final var newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).
                id(firstConfig.get().getId()).
                build();
        Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(newConfig)).findFirst();
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Save and flush by the config id with not the existed config attributes table")
    void saveAndFlushConfigByIdWithNotExistedConfigAttributesTable() throws SQLException {
        final var firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final var newConfig = new Config.Builder(getConfigWithSubProperties(NEW_CONFIG)).
                id(firstConfig.get().getId()).
                build();
        dropConfigAttributesTables();
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
        createRepository();
    }

    @Test
    @DisplayName("Save and flush by the config id with not the existed property attributes table")
    void saveAndFlushConfigByIdWithNotExistedPropertyAttributesTable() throws SQLException {
        final var firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final var newConfig = new Config.Builder(getConfigWithSubProperties(NEW_CONFIG)).
                id(firstConfig.get().getId()).
                build();
        dropConfigAttributesTables();
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
        createRepository();
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

    @Test
    @DisplayName("Save and flush an updated config with two properties")
    void saveAndFlushUpdatedConfigWithTwoProperties() {
        final var firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final var newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep(1);
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final var secondProperty = new Property.Builder("Property-2", "Value-2").
                attribute("key_2", "value_2").build();
        final var copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                property(new String[]{"Property-1"}, secondProperty).build();
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperty("Property-1").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-1", "Property-2").ifPresent(p -> assertTrue(p.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated config with three properties")
    void saveAndFlushUpdatedConfigWithThreeProperties() {
        final var firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final var newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep(1);
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final var thirdProperty = new Property.Builder("Property-3", "Value-3").
                attribute("key_3", "value_3").build();
        final var copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                property(new String[]{"Property-1", "Property-2"}, thirdProperty).build();
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperty("Property-1").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-1", "Property-2").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-1", "Property-2", "Property-3").ifPresent(p -> assertTrue(p.getId() > 0));
    }

    @Test
    @DisplayName("Optimistic locking error")
    void optimisticLockingError() {
        final var firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        final var newConfig = new Config.Builder(firstConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                build();
        configRepository.saveAndFlush(Stream.of(newConfig));
        assertThrows(RuntimeException.class, () -> configRepository.saveAndFlush(Stream.of(newConfig)));
    }

    @Test
    @DisplayName("Save and flush an updated config with one properties")
    void saveAndFlushUpdatedConfigWithOneProperties() {
        final var firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final var newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep(1);
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final var properties = new ArrayList<Property>(2);
        newConfig.get().getProperty("Property-1").ifPresent(properties::add);
        properties.add(new Property.Builder("Property-2", "Value-2").attribute("key_2", "value_2").build());
        final var copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                properties(properties).build();
        final var updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperty("Property-1").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-2").ifPresent(p -> assertTrue(p.getId() > 0));
    }

    private static void execute(final Statement statement, final String table) throws SQLException {
        statement.executeUpdate("DROP TABLE " + table + ";");
    }
}
