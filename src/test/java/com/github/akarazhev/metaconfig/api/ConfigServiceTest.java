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
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Config service test")
final class ConfigServiceTest extends UnitTest {
    private static DbServer dbServer;
    private static ConnectionPool connectionPool;
    private static ConfigService configService;

    @BeforeAll
    static void beforeAll() throws Exception {
        if (dbServer == null) {
            dbServer = DbServers.newServer().start();
        }

        if (connectionPool == null) {
            connectionPool = ConnectionPools.newPool();
        }

        if (configService == null) {
            final ConfigRepository configRepository =
                    new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
            configService = new ConfigServiceImpl.Builder(configRepository).build();
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        configService = null;

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
        configService.update(Stream.of(getConfigWithSubProperties(FIRST_CONFIG),
                getConfigWithProperties(SECOND_CONFIG)));
        configService.addConsumer(null);
    }

    @AfterEach
    void afterEach() {
        configService.remove(Stream.of(FIRST_CONFIG, SECOND_CONFIG, NEW_CONFIG));
    }

    @Test
    @DisplayName("Get configs by empty names")
    void getByEmptyNames() {
        // Check test results
        assertEquals(0, configService.get(Stream.empty()).count());
    }

    @Test
    @DisplayName("Get configs by the not existed name")
    void getByNotExistedName() {
        // Check test results
        assertEquals(0, configService.get(Stream.of(NEW_CONFIG)).count());
    }

    @Test
    @DisplayName("Get configs by names")
    void getConfigsByNames() {
        final Config[] configs = configService.get(Stream.of(FIRST_CONFIG, SECOND_CONFIG)).toArray(Config[]::new);
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
    @DisplayName("Get configs by names with the closed connection pool")
    void getByNamesWithClosedConnectionPool() throws IOException {
        connectionPool.close();
        // Check test results
        assertThrows(RuntimeException.class, () -> configService.get(Stream.of(FIRST_CONFIG, SECOND_CONFIG)));
        connectionPool = ConnectionPools.newPool();
        final ConfigRepository configRepository =
                new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
        configService = new ConfigServiceImpl.Builder(configRepository).build();
    }

    @Test
    @DisplayName("Get config names")
    void getNames() {
        final String[] names = configService.getNames().toArray(String[]::new);
        // Check test results
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    @Test
    @DisplayName("Get config names with the closed connection pool")
    void getNamesWithClosedConnectionPool() throws IOException {
        connectionPool.close();
        // Check test results
        assertThrows(RuntimeException.class, () -> configService.getNames());
        connectionPool = ConnectionPools.newPool();
        final ConfigRepository configRepository =
                new DbConfigRepository.Builder(connectionPool.getDataSource()).build();
        configService = new ConfigServiceImpl.Builder(configRepository).build();
    }

    @Test
    @DisplayName("Get configs")
    void getConfigs() {
        final Config[] configs = configService.get().toArray(Config[]::new);
        // Check test results
        assertEquals(2, configs.length);
        assertEquals(FIRST_CONFIG, configs[0].getName());
        assertEquals(SECOND_CONFIG, configs[1].getName());
    }

    @Test
    @DisplayName("Update a new config")
    void updateNewConfig() {
        final Optional<Config> newConfig =
                configService.update(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newConfig.isPresent());
        assertTrue(newConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Update an empty")
    void updateEmptyConfig() {
        // Check test results
        assertEquals(0, configService.update(Stream.empty()).count());
    }

    @Test
    @DisplayName("Update by the config id")
    void updateConfigById() {
        final Optional<Config> firstConfig = configService.get(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).
                id(firstConfig.get().getId()).
                build();
        Optional<Config> updatedConfig = configService.update(Stream.of(newConfig)).findFirst();
        assertTrue(updatedConfig.isPresent());
        assertTrue(updatedConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Optimistic locking error")
    void optimisticLockingError() {
        final Optional<Config> firstConfig = configService.get(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(firstConfig.get()).build();
        configService.update(Stream.of(newConfig));
        assertThrows(RuntimeException.class, () -> configService.update(Stream.of(newConfig)));
    }

    @Test
    @DisplayName("Remove configs by empty names")
    void removeByEmptyNames() {
        // Check test results
        assertEquals(0, configService.remove(Stream.empty()));
    }

    @Test
    @DisplayName("Remove configs by the not existed name")
    void removeByNotExistedName() {
        // Check test results
        assertEquals(0, configService.remove(Stream.of(NEW_CONFIG)));
    }

    @Test
    @DisplayName("Remove configs by names")
    void removeByNames() {
        // Check test results
        assertEquals(2, configService.remove(Stream.of(FIRST_CONFIG, SECOND_CONFIG)));
    }

    @Test
    @DisplayName("Add a consumer for the config")
    void addConsumer() {
        final StringBuilder message = new StringBuilder();
        configService.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });
        // Check test results
        assertEquals(0, message.length());
    }

    @Test
    @DisplayName("Accept config by the name")
    void acceptByName() {
        configService.accept(FIRST_CONFIG);
    }

    @Test
    @DisplayName("Accept config by the different name")
    void acceptByDifferentName() {
        final StringBuilder message = new StringBuilder();
        configService.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });

        configService.accept(NEW_CONFIG);
        // Check test results
        assertEquals(0, message.length());
    }

    @Test
    @DisplayName("Accept config by the name with consumer")
    void acceptByNameWithConsumer() {
        final StringBuilder message = new StringBuilder();
        configService.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });

        configService.accept(FIRST_CONFIG);
        // Check test results
        assertEquals(FIRST_CONFIG, message.toString());
    }
}
