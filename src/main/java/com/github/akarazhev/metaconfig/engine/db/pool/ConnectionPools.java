package com.github.akarazhev.metaconfig.engine.db.pool;

import com.github.akarazhev.metaconfig.api.Config;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.io.IOException;

public final class ConnectionPools {

    private ConnectionPools() {
        // Factory class
    }

    public static ConnectionPool newPool() {
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

    static ConnectionPool newPool(final Config config) {
        throw new RuntimeException("newPool is not implemented");
    }
}
