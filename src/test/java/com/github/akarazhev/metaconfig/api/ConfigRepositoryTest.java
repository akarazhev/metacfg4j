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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRepositoryTest {
    private static final String SIMPLE_CONFIG = "Simple Config";
    private static ConnectionPool connectionPool;
    private static ConfigRepository configRepository;
    private static DbServer dbServer;
    private int id;

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
        final Config simpleConfig = new Config.Builder(SIMPLE_CONFIG, Collections.emptyList()).build();
        configRepository.saveAndFlush(simpleConfig).findFirst().ifPresent(config -> id = config.getId());
    }

    @AfterEach
    void afterEach() {
        configRepository.delete(id);
    }

    @Test
    void findConfigByName() {
        final Optional<Config> config = configRepository.findByName(SIMPLE_CONFIG).findFirst();
        // Check test results
        assertTrue(config.isPresent());
        assertEquals(SIMPLE_CONFIG, config.get().getName());
    }
}
