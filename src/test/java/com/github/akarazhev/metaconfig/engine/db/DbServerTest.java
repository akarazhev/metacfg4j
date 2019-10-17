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
        dbServer = DbServers.newServer();
        dbServer.start();
    }

    @AfterAll
    static void afterAll() {
        dbServer.stop();
    }

    @Test
    void getPublicSchema() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        // add application code here
        assertEquals("PUBLIC", connection.getSchema());
        connection.close();
    }
}
