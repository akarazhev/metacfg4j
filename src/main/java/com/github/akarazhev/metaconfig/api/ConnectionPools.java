package com.github.akarazhev.metaconfig.api;

import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.io.IOException;

final class ConnectionPools {

    private ConnectionPools() {
        // Factory class
    }

    static ConnectionPool newConnectionPool() {
        return new ConnectionPool() {
            private final JdbcConnectionPool cp =
                    JdbcConnectionPool.create("jdbc:h2:~/test", "sa", "sa");

            @Override
            public DataSource getDataSource() {
                return cp;
            }

            @Override
            public void close() throws IOException {
                cp.dispose();
            }
        };
    }

    static ConnectionPool newConnectionPool(final Config config) {
        throw new RuntimeException("newServer is not implemented");
    }
}
