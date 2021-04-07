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
import com.github.akarazhev.metaconfig.engine.web.WebClient;
import com.github.akarazhev.metaconfig.engine.web.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Meta config test")
final class MetaConfigTest extends UnitTest {
    private static DbServer dbServer;
    private static ConnectionPool connectionPool;
    private static MetaConfig dbMetaConfig;
    private static MetaConfig webMetaConfig;

    @BeforeAll
    static void beforeAll() throws Exception {
        if (dbServer == null) {
            dbServer = DbServers.newServer().start();
        }

        if (connectionPool == null) {
            connectionPool = ConnectionPools.newPool();
        }

        if (dbMetaConfig == null) {
            final Config webServer = new Config.Builder(Server.Settings.CONFIG_NAME,
                    Arrays.asList(
                            new Property.Builder(Server.Settings.HOSTNAME, "localhost").build(),
                            new Property.Builder(Server.Settings.PORT, 8000).build(),
                            new Property.Builder(Server.Settings.BACKLOG, 0).build(),
                            new Property.Builder(Server.Settings.KEY_STORE_FILE, "./data/metacfg4j.keystore").build(),
                            new Property.Builder(Server.Settings.ALIAS, "alias").build(),
                            new Property.Builder(Server.Settings.STORE_PASSWORD, "password").build(),
                            new Property.Builder(Server.Settings.KEY_PASSWORD, "password").build()))
                    .build();
            dbMetaConfig = new MetaConfig.Builder().
                    webServer(webServer).
                    dataSource(connectionPool.getDataSource()).
                    build();
        }

        if (webMetaConfig == null) {
            final Config webClient = new Config.Builder(WebClient.Settings.CONFIG_NAME,
                    Arrays.asList(
                            new Property.Builder(WebClient.Settings.URL, "https://localhost:8000/api/metacfg").build(),
                            new Property.Builder(WebClient.Settings.ACCEPT_ALL_HOSTS, true).build()))
                    .build();
            webMetaConfig = new MetaConfig.Builder().
                    webClient(webClient).
                    build();
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        if (webMetaConfig != null) {
            webMetaConfig.close();
            webMetaConfig = null;
        }

        if (dbMetaConfig != null) {
            dbMetaConfig.close();
            dbMetaConfig = null;
        }

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
        dbMetaConfig.update(Stream.of(getConfigWithSubProperties(FIRST_CONFIG),
                getConfigWithProperties(SECOND_CONFIG)));
        dbMetaConfig.addConsumer(null);
    }

    @AfterEach
    void afterEach() {
        dbMetaConfig.remove(Stream.of(FIRST_CONFIG, SECOND_CONFIG, NEW_CONFIG));
    }

    @Test
    @DisplayName("Build default config")
    void buildDefaultConfig() {
        assertThrows(RuntimeException.class, () -> new MetaConfig.Builder().
                defaultConfig().
                dataSource(connectionPool.getDataSource()).
                build());
    }

    @Test
    @DisplayName("Get configs by empty names")
    void getByEmptyNames() {
        // Check test results
        assertEquals(0, dbMetaConfig.get(Stream.empty()).count());
        assertEquals(0, webMetaConfig.get(Stream.empty()).count());
    }

    @Test
    @DisplayName("Get configs by the not existed name")
    void getByNotExistedName() {
        // Check test results
        assertEquals(0, dbMetaConfig.get(Stream.of(NEW_CONFIG)).count());
        assertEquals(0, webMetaConfig.get(Stream.of(NEW_CONFIG)).count());
    }

    @Test
    @DisplayName("Get configs by names")
    void getConfigsByNames() {
        assertEqualsConfigs(dbMetaConfig.get(Stream.of(FIRST_CONFIG, SECOND_CONFIG)).toArray(Config[]::new));
        assertEqualsConfigs(webMetaConfig.get(Stream.of(FIRST_CONFIG, SECOND_CONFIG)).toArray(Config[]::new));
    }

    @Test
    @DisplayName("Get config with empty attributes")
    void getConfigWithEmptyAttributes() {
        final Property property = new Property.Builder("Property", "value").build();
        final Config newConfig = new Config.Builder(NEW_CONFIG, Collections.singleton(property)).build();
        dbMetaConfig.update(Stream.of(newConfig));
        final Optional<Config> dbConfig = dbMetaConfig.get(Stream.of(NEW_CONFIG)).findFirst();
        assertTrue(dbConfig.isPresent());
        assertEquals(NEW_CONFIG, dbConfig.get().getName());
    }

    @Test
    @DisplayName("Get config names")
    void getNames() {
        assertEqualsNames(dbMetaConfig.getNames().toArray(String[]::new));
        assertEqualsNames(webMetaConfig.getNames().toArray(String[]::new));
    }

    @Test
    @DisplayName("Get config names by a page request")
    void getNamesByPageRequest() {
        final PageResponse dbPageResponse = dbMetaConfig.getNames(new PageRequest.Builder(CONFIG).build());
        // Check test results
        assertEquals(0, dbPageResponse.getPage());
        assertEquals(2, dbPageResponse.getTotal());
        assertEqualsNames(dbPageResponse.getNames().toArray(String[]::new));
        final PageResponse webPageResponse = webMetaConfig.getNames(new PageRequest.Builder(CONFIG).build());
        // Check test results
        assertEquals(0, webPageResponse.getPage());
        assertEquals(2, webPageResponse.getTotal());
        assertEqualsNames(webPageResponse.getNames().toArray(String[]::new));
    }

    @Test
    @DisplayName("Get configs")
    void getConfigs() {
        assertEqualsNames(dbMetaConfig.get().toArray(Config[]::new));
        assertEqualsNames(webMetaConfig.get().toArray(Config[]::new));
    }

    @Test
    @DisplayName("Update a new config")
    void updateNewConfig() {
        final Optional<Config> newDbConfig =
                dbMetaConfig.update(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        final Optional<Config> newWebConfig =
                webMetaConfig.update(Stream.of(getConfigWithProperties(NEW_CONFIG))).findFirst();
        // Check test results
        assertTrue(newDbConfig.isPresent());
        assertTrue(newDbConfig.get().getId() > 0);
        assertTrue(newWebConfig.isPresent());
        assertTrue(newWebConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Update an empty")
    void updateEmptyConfig() {
        // Check test results
        assertEquals(0, dbMetaConfig.update(Stream.empty()).count());
        assertEquals(0, webMetaConfig.update(Stream.empty()).count());
    }

    @Test
    @DisplayName("Update by the first config id")
    void updateConfigByFirstId() {
        final Optional<Config> firstConfig = dbMetaConfig.get(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).
                id(firstConfig.get().getId()).
                build();
        Optional<Config> updatedDbConfig = dbMetaConfig.update(Stream.of(newConfig)).findFirst();
        assertTrue(updatedDbConfig.isPresent());
        assertTrue(updatedDbConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Update by the second config id")
    void updateConfigBySecondId() {
        final Optional<Config> secondConfig = dbMetaConfig.get(Stream.of(FIRST_CONFIG)).findFirst();
        // Check test results
        assertTrue(secondConfig.isPresent());
        final Config newConfig = new Config.Builder(NEW_CONFIG, Collections.emptyList()).
                id(secondConfig.get().getId()).
                build();
        Optional<Config> updatedWebConfig = webMetaConfig.update(Stream.of(newConfig)).findFirst();
        assertTrue(updatedWebConfig.isPresent());
        assertTrue(updatedWebConfig.get().getId() > 0);
    }

    @Test
    @DisplayName("Optimistic locking error")
    void optimisticLockingError() {
        final Optional<Config> firstConfig = dbMetaConfig.get(Stream.of(FIRST_CONFIG)).findFirst();
        assertTrue(firstConfig.isPresent());
        final Config newConfig = new Config.Builder(firstConfig.get()).
                updated(Clock.systemDefaultZone().millis()).
                build();
        dbMetaConfig.update(Stream.of(newConfig));
        assertThrows(RuntimeException.class, () -> dbMetaConfig.update(Stream.of(newConfig)));
        assertThrows(RuntimeException.class, () -> webMetaConfig.update(Stream.of(newConfig)));
    }

    @Test
    @DisplayName("Remove configs by empty names")
    void removeByEmptyNames() {
        // Check test results
        assertEquals(0, dbMetaConfig.remove(Stream.empty()));
        assertEquals(0, webMetaConfig.remove(Stream.empty()));
    }

    @Test
    @DisplayName("Remove configs by the not existed name")
    void removeByNotExistedName() {
        // Check test results
        assertEquals(0, dbMetaConfig.remove(Stream.of(NEW_CONFIG)));
        assertEquals(0, webMetaConfig.remove(Stream.of(NEW_CONFIG)));
    }

    @Test
    @DisplayName("Remove configs by names")
    void removeByNames() {
        // Check test results
        assertEquals(1, dbMetaConfig.remove(Stream.of(FIRST_CONFIG)));
        assertEquals(1, webMetaConfig.remove(Stream.of(SECOND_CONFIG)));
    }

    @Test
    @DisplayName("Add a consumer for the config")
    void addConsumer() {
        final StringBuilder message = new StringBuilder();
        dbMetaConfig.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });
        // Check test results
        assertEquals(0, message.length());
    }

    @Test
    @DisplayName("Accept config by names")
    void acceptByNames() {
        dbMetaConfig.accept(Stream.of(FIRST_CONFIG));
    }

    @Test
    @DisplayName("Accept config by names")
    void acceptByDifferentNames() {
        final StringBuilder message = new StringBuilder();
        dbMetaConfig.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });

        webMetaConfig.accept(Stream.of(NEW_CONFIG));
        // Check test results
        assertEquals(0, message.length());
    }

    @Test
    @DisplayName("Accept config by names with consumer")
    void acceptByNamesWithConsumer() {
        final StringBuilder message = new StringBuilder();
        dbMetaConfig.addConsumer(config -> {
            if (FIRST_CONFIG.equals(config.getName())) {
                message.append(FIRST_CONFIG);
            }
        });

        webMetaConfig.accept(Stream.of(FIRST_CONFIG));
        // Check test results
        assertEquals(FIRST_CONFIG, message.toString());
    }

    private void assertEqualsConfigs(final Config[] configs) {
        // Check test results
        assertEquals(2, configs.length);
        final Config firstExpected = getConfigWithSubProperties(FIRST_CONFIG);
        final Config secondExpected = getConfigWithSubProperties(SECOND_CONFIG);
        assertEqualsConfig(firstExpected, configs[0]);
        assertEqualsProperty(firstExpected, configs[0]);
        assertEqualsConfig(secondExpected, configs[1]);
        assertEqualsProperty(secondExpected, configs[1]);
    }

    private void assertEqualsNames(final String[] names) {
        assertEquals(2, names.length);
        assertEquals(FIRST_CONFIG, names[0]);
        assertEquals(SECOND_CONFIG, names[1]);
    }

    private void assertEqualsNames(final Config[] configs) {
        assertEquals(2, configs.length);
        assertEquals(FIRST_CONFIG, configs[0].getName());
        assertEquals(SECOND_CONFIG, configs[1].getName());
    }
}
