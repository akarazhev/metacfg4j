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
package com.github.akarazhev.metaconfig.engine.db.pool;

import com.github.akarazhev.metaconfig.api.Config;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.io.IOException;

import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_FACTORY_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.IMPLEMENTATION_NOT_PROVIDED;

/**
 * Provides factory methods to create a connection pool.
 */
public final class ConnectionPools {

    private ConnectionPools() {
        throw new AssertionError(CREATE_FACTORY_CLASS_ERROR);
    }

    /**
     * Returns a default connection pool.
     *
     * @return a connection pool.
     */
    public static ConnectionPool newPool() {
        return new ConnectionPool() {
            private final JdbcConnectionPool cp =
                    JdbcConnectionPool.create("jdbc:h2:./data/metacfg4j", "sa", "sa");

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

    /**
     * Returns a connection pool based on the configuration.
     *
     * @return a connection pool.
     */
    public static ConnectionPool newPool(final Config config) {
        throw new RuntimeException(IMPLEMENTATION_NOT_PROVIDED);
    }
}
