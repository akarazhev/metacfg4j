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
package com.github.akarazhev.metaconfig.engine.db;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbServerTest {
    private static DbServer dbServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        if (dbServer == null) {
            dbServer = DbServers.newServer().start();
        }
    }

    @AfterAll
    static void afterAll() {
        if (dbServer != null) {
            dbServer.stop();
            dbServer = null;
        }
    }

    @Test
    void getPublicSchema() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:./data/metacfg4j", "sa", "sa");
        // add application code here
        assertEquals("PUBLIC", connection.getSchema());
        connection.close();
    }
}
