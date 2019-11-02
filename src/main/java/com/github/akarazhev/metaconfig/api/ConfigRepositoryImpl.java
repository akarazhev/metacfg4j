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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.CONFIG_ID_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_CONFIG_TABLE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_ID_VALUE;

/**
 * {@inheritDoc}
 */
final class ConfigRepositoryImpl implements ConfigRepository {
    private final DataSource dataSource;

    private ConfigRepositoryImpl(final Builder builder) {
        this.dataSource = builder.dataSource;
        init(this.dataSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByName(final String name) {
        try {
            final String sql = "SELECT `ID`, `NAME`, `DESCRIPTION`, `VERSION`, `UPDATED` FROM `CONFIGS` WHERE `NAME` = ?";
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                try (final ResultSet resultSet = statement.executeQuery()) {
                    final List<Config> configs = new LinkedList<>();
                    if (resultSet.next()) {
                        configs.add(new Config.Builder(resultSet.getString(2), Collections.emptyList()).
                                id(resultSet.getInt(1)).
                                description(resultSet.getString(3)).
                                version(resultSet.getInt(4)).
                                updated(resultSet.getLong(5)).
                                build());
                    }

                    return configs.stream();
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(String.format(RECEIVED_CONFIG_ERROR, name), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        try {
            final String sql = "SELECT `NAME` FROM `CONFIGS`";
            try (final Connection connection = dataSource.getConnection();
                 final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                final List<String> names = new LinkedList<>();
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }

                return names.stream();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(RECEIVED_CONFIGS_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config saveAndFlush(final Config config) {
        if (config.getId() > 1) {
            try {
                // Implement transaction support, batch push, rollbacks, optimistic locking
                return updateConfig(config);
            } catch (final SQLException e) {
                throw new RuntimeException(String.format(UPDATE_CONFIG_ERROR, config.getName()), e);
            }
        } else {
            try {
                // Implement transaction support, batch push, rollbacks, optimistic locking
                return insertConfig(config);
            } catch (final SQLException e) {
                throw new RuntimeException(String.format(INSERT_CONFIG_ERROR, config.getName()), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int id) {
        if (id > 0) {
            try {
                final String sql = "DELETE FROM `CONFIGS` WHERE `ID` = ?";
                try (final Connection connection = dataSource.getConnection();
                     final PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, id);
                    statement.executeUpdate();
                }
            } catch (final SQLException e) {
                throw new RuntimeException(DELETE_CONFIG_ERROR, e);
            }
        } else {
            throw new RuntimeException(WRONG_ID_VALUE);
        }
    }

    private void init(final DataSource dataSource) {
        Connection connection = null;
        try {
            connection = JDBCUtils.open(dataSource);
            createTables(connection);
        } catch (final SQLException e) {
            JDBCUtils.handle(connection, e);
        } finally {
            JDBCUtils.handle(connection);
        }
    }

    private void createTables(final Connection connection) throws SQLException {
        try {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CONFIGS` " +
                        "(`ID` IDENTITY NOT NULL, " +
                        "`NAME` VARCHAR(255) NOT NULL, " +
                        "`DESCRIPTION` VARCHAR(1024), " +
                        "`VERSION` INT NOT NULL, " +
                        "`UPDATED` BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CONFIG_ATTRIBUTES` " +
                        "(`ID` IDENTITY NOT NULL, " +
                        "`CONFIG_ID` BIGINT NOT NULL, " +
                        "`KEY` VARCHAR(255) NOT NULL, " +
                        "`VALUE` VARCHAR(1024))");
                connection.commit();
            }
        } catch (SQLException e) {
            throw new SQLException(CREATE_CONFIG_TABLE_ERROR, e);
        }
    }

    private Config insertConfig(final Config config) throws SQLException {
        final String sql = "INSERT INTO `CONFIGS` (`NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (?, ?, ?, ?)";
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setStatement(config, statement);
            if (statement.executeUpdate() > 0) {
                try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        return new Config.Builder(config).id(resultSet.getInt(1)).build();
                    } else {
                        throw new RuntimeException(CONFIG_ID_ERROR);
                    }
                }
            } else {
                throw new RuntimeException(String.format(INSERT_CONFIG_ERROR, config.getName()));
            }
        }
    }

    private Config updateConfig(final Config config) throws SQLException {
        final String sql =
                "UPDATE `CONFIGS` SET `NAME` = ?, `DESCRIPTION` = ?, `VERSION` = ?, `UPDATED` = ? WHERE `ID` = ?";
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setStatement(config, statement);
            statement.setLong(5, config.getId());
            if (statement.executeUpdate() > 0) {
                return config;
            } else {
                throw new RuntimeException(String.format(UPDATE_CONFIG_ERROR, config.getName()));
            }
        }
    }

    private void setStatement(final Config config, final PreparedStatement statement) throws SQLException {
        statement.setString(1, config.getName());
        statement.setString(2, config.getDescription());
        statement.setLong(3, config.getVersion());
        statement.setLong(4, config.getUpdated());
    }

    private static final class JDBCUtils {

        private static Connection open(final DataSource dataSource) throws SQLException {
            final Connection connection = Objects.requireNonNull(dataSource).getConnection();
            connection.setAutoCommit(false);
            return connection;
        }

        private static void handle(final Connection connection, final SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException("", e); //
                }
            }
        }

        private static void handle(final Connection connection) {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(""); //
                }
            }
        }
    }

    /**
     * Wraps and builds the instance of the config repository.
     */
    public final static class Builder {
        private final DataSource dataSource;

        /**
         * Constructs a config repository with a required parameter.
         *
         * @param dataSource a datasource.
         */
        public Builder(final DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Builds a config repository with a required parameter.
         *
         * @return a builder of the config repository.
         */
        public ConfigRepository build() {
            return new ConfigRepositoryImpl(this);
        }
    }
}
