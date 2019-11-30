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
import com.github.akarazhev.metaconfig.engine.web.WebClient;
import com.github.akarazhev.metaconfig.engine.web.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.Arrays;

@DisplayName("Meta config test")
final class MetaConfigTest {
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
                    defaultConfig().
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
}
