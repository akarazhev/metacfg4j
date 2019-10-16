package com.github.akarazhev.metaconfig.store;

import com.github.akarazhev.metaconfig.Config;
import com.github.akarazhev.metaconfig.store.h2db.H2dbServer;

import java.sql.SQLException;

public final class DbServers {

    private DbServers() {
    }

    public static DbServer newDefaultDbServer() throws SQLException {
        return new H2dbServer();
    }

    public static DbServer newDbServer(final Config config) throws SQLException {
        return new H2dbServer(config) ;
    }
}
