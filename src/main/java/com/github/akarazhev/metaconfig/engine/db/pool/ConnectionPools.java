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
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.extension.Validator;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.util.Arrays;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_FACTORY_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;

/**
 * Provides factory methods to create a connection pool.
 */
public final class ConnectionPools {

    private ConnectionPools() {
        throw new AssertionError(CREATE_FACTORY_CLASS_ERROR);
    }

    /**
     * Settings constants for the connection pool.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        public static final String CONFIG_NAME = "connection-pool";
        // The url key
        public static final String URL = "url";
        // The url value
        static final String URL_VALUE = "jdbc:h2:./data/metacfg4j";
        // The user key
        public static final String USER = "user";
        // The user value
        static final String USER_VALUE = "sa";
        // The password key
        public static final String PASSWORD = "password";
        // The password value
        static final String PASSWORD_VALUE = "sa";
    }

    /**
     * Returns a default connection pool.
     *
     * @return a connection pool.
     */
    public static ConnectionPool newPool() {
        return newPool(new Config.Builder(Settings.CONFIG_NAME, Arrays.asList(
                new Property.Builder(Settings.URL, Settings.URL_VALUE).build(),
                new Property.Builder(Settings.USER, Settings.USER_VALUE).build(),
                new Property.Builder(Settings.PASSWORD, Settings.PASSWORD_VALUE).build())).build());
    }

    /**
     * Returns a connection pool based on the configuration.
     *
     * @return a connection pool.
     */
    public static ConnectionPool newPool(final Config config) {
        // Validate the config
        final Config poolConfig = Validator.of(config).
                validate(c -> Settings.CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(Settings.URL).isPresent(), "URL is not presented.").
                validate(c -> c.getProperty(Settings.USER).isPresent(), "User is not presented.").
                validate(c -> c.getProperty(Settings.PASSWORD).isPresent(), "Password is not presented.").
                get();
        // Get the url
        final String url = poolConfig.getProperty(Settings.URL).
                map(Property::getValue).
                orElse(Settings.URL_VALUE);
        // Get the user
        final String user = poolConfig.getProperty(Settings.USER).
                map(Property::getValue).
                orElse(Settings.USER_VALUE);
        // Get the password
        final String password = poolConfig.getProperty(Settings.PASSWORD).
                map(Property::getValue).
                orElse(Settings.PASSWORD_VALUE);
        // Create the connection pool
        return new ConnectionPool() {
            private final JdbcConnectionPool connectionPool = JdbcConnectionPool.create(url, user, password);

            @Override
            public void close() {
                connectionPool.dispose();
            }

            @Override
            public DataSource getDataSource() {
                return connectionPool;
            }
        };
    }
}
