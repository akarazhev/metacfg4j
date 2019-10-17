package com.github.akarazhev.metaconfig.engine.db;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.engine.db.h2db.H2dbServer;

import java.sql.SQLException;

public final class DbServers {

    private DbServers() {
        // Factory class
    }

    public static DbServer newServer() throws SQLException {
        return new H2dbServer();
    }

    public static DbServer newServer(final Config config) throws SQLException {
        return new H2dbServer(config) ;
    }
}
