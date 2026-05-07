/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.UnitTest;
import com.github.akarazhev.metaconfig.engine.db.pool.ConnectionPool;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Settings.DB_DIALECT;
import static com.github.akarazhev.metaconfig.Constants.Settings.FETCH_SIZE;
import static com.github.akarazhev.metaconfig.Constants.Settings.POSTGRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Data base repository test")
@Testcontainers
final class PostgresDbConfigRepositoryTest extends UnitTest {
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("mydb")
            .withUsername("myuser")
            .withPassword("mypass");
    private static ConfigRepository configRepository;

    @BeforeAll
    static void beforeAll() {
        if (configRepository == null) {
            createRepository();
        }
    }

    @AfterAll
    static void afterAll() {
        if (configRepository != null) {
            configRepository = null;
        }

        if (postgreSQLContainer != null) {
            postgreSQLContainer.close();
            postgreSQLContainer = null;
        }
    }

    private static void createRepository() {
        final Map<String, String> mapping = new HashMap<>();
        mapping.put("configs", "CONFIGS");
        mapping.put("config-attributes", "CONFIG_ATTRIBUTES");
        mapping.put("properties", "PROPERTIES");
        mapping.put("property-attributes", "PROPERTY_ATTRIBUTES");

        final HashMap<String, Object> settings = new HashMap<>();
        settings.put(FETCH_SIZE, 100);
        settings.put(DB_DIALECT, POSTGRE);

        configRepository =
                new DbConfigRepository.Builder(getConnectionPool().getDataSource()).mapping(mapping).settings(settings).build();
    }

    private static ConnectionPool getConnectionPool() {
        return new ConnectionPool() {
            private final JdbcConnectionPool connectionPool = JdbcConnectionPool.create(setUpConnectionPool());

            @Override
            public DataSource getDataSource() {
                return connectionPool;
            }

            @Override
            public void close() {
                connectionPool.dispose();
            }
        };
    }

    private static PGConnectionPoolDataSource setUpConnectionPool() {
        PGConnectionPoolDataSource pgConnectionPoolDataSource = new PGConnectionPoolDataSource();
        pgConnectionPoolDataSource.setUrl(postgreSQLContainer.getJdbcUrl());
        pgConnectionPoolDataSource.setUser(postgreSQLContainer.getUsername());
        pgConnectionPoolDataSource.setPassword(postgreSQLContainer.getPassword());
        pgConnectionPoolDataSource.setDatabaseName(postgreSQLContainer.getDatabaseName());
        return pgConnectionPoolDataSource;
    }

    private static void dropConfigAttributesTables() throws SQLException {
        try (final Connection connection = getConnectionPool().getDataSource().getConnection();
             final Statement statement = connection.createStatement()) {
            execute(statement, "CONFIG_ATTRIBUTES");
        }
    }

    private static void dropPropertyAttributesTables() throws SQLException {
        try (final Connection connection = getConnectionPool().getDataSource().getConnection();
             final Statement statement = connection.createStatement()) {
            execute(statement, "PROPERTY_ATTRIBUTES");
        }
    }

    private static void dropTables() throws SQLException {
        try (final Connection connection = getConnectionPool().getDataSource().getConnection();
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
        Config[] configs = configRepository.findByNames(Stream.of(FIRST_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(1, configs.length);
        final Config firstExpected = getConfigWithSubProperties(FIRST_CONFIG);
        assertEqualsConfig(firstExpected, configs[0]);
        assertEqualsProperty(firstExpected, configs[0]);

        configs = configRepository.findByNames(Stream.of(SECOND_CONFIG)).toArray(Config[]::new);
        // Check test results
        assertEquals(1, configs.length);
        final Config secondExpected = getConfigWithSubProperties(SECOND_CONFIG);
        assertEqualsConfig(secondExpected, configs[0]);
        assertEqualsProperty(secondExpected, configs[0]);
    }

    @Test
    @DisplayName("Find all configs by names")
    void findAllConfigsByNames() {
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
    @DisplayName("Find config names by a page request")
    void findByPageRequest() {
        final PageResponse page = configRepository.findByPageRequest(new PageRequest.Builder(CONFIG).build());
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(2, page.getTotal());
        final String[] names = page.getNames().toArray(String[]::new);
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names by a page request")
    void findByPageRequestAndAttributes() {
        final PageRequest request = new PageRequest.Builder(CONFIG).
                attributes(Collections.singletonMap("key", "value")).
                build();
        final PageResponse page = configRepository.findByPageRequest(request);
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(2, page.getTotal());
        final String[] names = page.getNames().toArray(String[]::new);
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Find config names by a name, page, size and sorting")
    void findByNameAndPageAndSizeAndSorting() {
        final PageRequest request = new PageRequest.Builder(CONFIG).
                page(1).
                size(1).
                attribute("key", "value").
                ascending(false).
                build();
        final PageResponse page = configRepository.findByPageRequest(request);
        // Check test results
        assertEquals(1, page.getPage());
        assertEquals(2, page.getTotal());
        final String[] names = page.getNames().toArray(String[]::new);
        assertEquals(1, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
    }

    @Test
    @DisplayName("Find config names by a wrong name")
    void findByWrongName() {
        final PageResponse page = configRepository.findByPageRequest(new PageRequest.Builder(NEW_CONFIG).build());
        // Check test results
        assertEquals(0, page.getPage());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getNames().count());
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
    @DisplayName("Save and flush a new config without properties")
    void saveAndFlushNewConfigWithoutProperties() {
        Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).build();
        configRepository.saveAndFlush(Stream.of(newConfig));
        final Optional<Config> updatedConfig = configRepository.findByNames(Stream.of(NEW_CONFIG)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        newConfig = updatedConfig.get();
        assertTrue(newConfig.getId() > 0);
        assertEquals(0, newConfig.getProperties().count());
    }

    @Test
    @DisplayName("Save and read a large config with properties")
    void saveAndReadLargeConfigWithProperties() {
        // Save a large config
        System.out.println("Start saveAndFlush");
        long time = System.currentTimeMillis();
        Optional<Config> config = configRepository.saveAndFlush(Stream.of(getLargeConfig())).findFirst();
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
        final Optional<Config> largeConfig = configRepository.saveAndFlush(Stream.of(getLargeConfig())).findFirst();
        System.out.println("End saveAndFlush in " + (System.currentTimeMillis() - time) + " ms.");
        sleep();
        // Check test results
        assertTrue(largeConfig.isPresent());
        // Update a large config
        final Config config = new Config.Builder(largeConfig.get()).
                properties(getProperties(1, 50)).
                updated(Clock.systemDefaultZone().millis()).
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
        sleep();
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
    @DisplayName("Save and flush an updated new config with property and empty attributes")
    void saveAndFlushUpdatedNewConfigWithPropertyAndEmptyAttributes() {
        final Config config = new Config.Builder(NEW_CONFIG, Collections.singletonList(getProperty())).
                attribute("key_1", "value_1").
                attribute("key_2", "value_2").
                attribute("key_3", "value_3").build();
        final Optional<Config> newConfig = configRepository.saveAndFlush(Stream.of(config)).findFirst();
        sleep();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(property -> assertTrue(property.getId() > 0));

        assertTrue(newConfig.get().getProperty("Property-1").isPresent());
        final Property firstProperty = new Property.Builder("Value", 1000).
                id(newConfig.get().getProperty("Property-1").get().getId()).build();
        final Config updateConfig = new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).
                id(newConfig.get().getId()).
                description("Description").build();
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

    @Test
    @DisplayName("Save and flush an updated config with one properties")
    void saveAndFlushUpdatedConfigWithOneProperties() {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final Optional<Config> newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep();
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final Collection<Property> properties = new ArrayList<>(2);
        newConfig.get().getProperty("Property-1").ifPresent(properties::add);
        properties.add(new Property.Builder("Property-2", "Value-2").attribute("key_2", "value_2").build());
        final Config copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                properties(properties).build();
        final Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperty("Property-1").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-2").ifPresent(p -> assertTrue(p.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated config with two properties")
    void saveAndFlushUpdatedConfigWithTwoProperties() {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final Optional<Config> newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep();
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final Property secondProperty = new Property.Builder("Property-2", "Value-2").
                attribute("key_2", "value_2").build();
        final Config copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                property(new String[]{"Property-1"}, secondProperty).build();
        final Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
        // Check test results
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
        updatedConfig.get().getProperty("Property-1").ifPresent(p -> assertTrue(p.getId() > 0));
        updatedConfig.get().getProperty("Property-1", "Property-2").ifPresent(p -> assertTrue(p.getId() > 0));
    }

    @Test
    @DisplayName("Save and flush an updated config with three properties")
    void saveAndFlushUpdatedConfigWithThreeProperties() {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final Optional<Config> newConfig = configRepository.saveAndFlush(
                Stream.of(new Config.Builder(NEW_CONFIG, Collections.singletonList(firstProperty)).build())
        ).findFirst();
        sleep();
        //  Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
        newConfig.get().getProperties().forEach(p -> assertTrue(p.getId() > 0));

        final Property thirdProperty = new Property.Builder("Property-3", "Value-3").
                attribute("key_3", "value_3").build();
        final Config copyConfig = new Config.Builder(newConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                property(new String[]{"Property-1", "Property-2"}, thirdProperty).build();
        final Optional<Config> updatedConfig = configRepository.saveAndFlush(Stream.of(copyConfig)).findFirst();
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
        final Optional<Config> firstConfig = configRepository.findByNames(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(firstConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                build();
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
}
