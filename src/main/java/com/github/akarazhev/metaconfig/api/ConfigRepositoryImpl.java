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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_CONFIG_TABLE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_CONNECTION_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ROLLBACK_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_CONFIGS_ERROR;

/**
 * {@inheritDoc}
 */
final class ConfigRepositoryImpl implements ConfigRepository {
    private final DataSource dataSource;

    private ConfigRepositoryImpl(final Builder builder) {
        // TODO: 1. implement sub-tables references
        // TODO: 2. implement optimistic locking
        this.dataSource = builder.dataSource;
        init(this.dataSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(final Stream<String> stream) {
        try {
            final StringBuilder sql = new StringBuilder("SELECT `ID`, `NAME`, `DESCRIPTION`, `VERSION`, `UPDATED` " +
                    "FROM `CONFIGS` WHERE `NAME` = ?");
            final String[] names = stream.toArray(String[]::new);
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> sql.append(" OR `NAME` = ?"));
            }

            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                JDBCUtils.setParameters(statement, names);

                try (final ResultSet resultSet = statement.executeQuery()) {
                    final List<Config> configs = new LinkedList<>();
                    while (resultSet.next()) {
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
            throw new RuntimeException(RECEIVED_CONFIGS_ERROR, e);
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
                final Set<String> names = new HashSet<>();
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
    public Stream<Config> saveAndFlush(final Stream<Config> stream) {
        Connection connection = null;
        try {
            connection = JDBCUtils.open(dataSource);
            return saveAndFlush(connection, stream);
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }

        return Stream.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(final Stream<String> stream) {
        Connection connection = null;
        try {
            connection = JDBCUtils.open(dataSource);
            final String[] names = stream.toArray(String[]::new);
            return delete(connection, names);
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }

        return 0;
    }

    private void init(final DataSource dataSource) {
        Connection connection = null;
        try {
            connection = JDBCUtils.open(dataSource);
            createTables(connection);
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }
    }

    private Stream<Config> saveAndFlush(final Connection connection, final Stream<Config> stream) throws SQLException {
        final List<Config> updateConfigs = new LinkedList<>();
        final List<Config> insertConfigs = new LinkedList<>();
        stream.forEach(config -> {
            if (config.getId() > 1) {
                updateConfigs.add(config);
            } else {
                insertConfigs.add(config);
            }
        });

        return Stream.concat(updateConfigs(connection, updateConfigs.toArray(new Config[0])),
                insertConfigs(connection, insertConfigs.toArray(new Config[0])));
    }

    private Stream<Config> insertConfigs(final Connection connection, final Config[] configs) throws SQLException {
        final Config[] inserted = new Config[configs.length];
        final String sql = "INSERT INTO `CONFIGS` (`NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (?, ?, ?, ?)";
        try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (final Config config : configs) {
                JDBCUtils.setParameters(statement, config);
                statement.addBatch();
            }

            if (statement.executeBatch().length == configs.length) {
                try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                    for (int i = 0; i < configs.length; i++) {
                        resultSet.absolute(i + 1);
                        inserted[i] = new Config.Builder(configs[i]).id(resultSet.getInt(1)).build();
                    }
                }
            } else {
                throw new SQLException(INSERT_CONFIGS_ERROR);
            }

            connection.commit();
        }

        return Arrays.stream(inserted);
    }

    private Stream<Config> updateConfigs(final Connection connection, final Config[] configs) throws SQLException {
        final String sql =
                "UPDATE `CONFIGS` SET `NAME` = ?, `DESCRIPTION` = ?, `VERSION` = ?, `UPDATED` = ? WHERE `ID` = ?";
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            for (final Config config : configs) {
                JDBCUtils.setParameters(statement, config);
                statement.setLong(5, config.getId());
                statement.addBatch();
            }

            if (statement.executeBatch().length != configs.length) {
                throw new SQLException(UPDATE_CONFIGS_ERROR);
            }

            connection.commit();
        }

        return Arrays.stream(configs);
    }

    private int delete(final Connection connection, final String[] names) throws SQLException {
        try {
            final StringBuilder sql = new StringBuilder("DELETE FROM `CONFIGS` WHERE `NAME` = ?");
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> sql.append(" OR `NAME` = ?"));
            }

            try (final PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                JDBCUtils.setParameters(statement, names);
                final int deleted = statement.executeUpdate();
                connection.commit();
                return deleted;
            }
        } catch (final SQLException e) {
            throw new SQLException(DELETE_CONFIGS_ERROR, e);
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
                        "`VALUE` VARCHAR(1024), " +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES CONFIGS(ID) ON DELETE CASCADE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `PROPERTIES` " +
                        "(`ID` IDENTITY NOT NULL, " +
                        "`PROPERTY_ID` BIGINT NOT NULL, " +
                        "`CONFIG_ID` BIGINT NOT NULL, " +
                        "`NAME` VARCHAR(255) NOT NULL, " +
                        "`CAPTION` VARCHAR(255), " +
                        "`DESCRIPTION` VARCHAR(1024), " +
                        "`TYPE` ENUM NOT NULL, " +
                        "`VALUE` VARCHAR(4096) NOT NULL, " +
                        "`VERSION` INT NOT NULL, " +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES CONFIGS(ID) ON DELETE CASCADE," +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES PROPERTIES(ID) ON DELETE CASCADE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `PROPERTY_ATTRIBUTES` " +
                        "(`ID` IDENTITY NOT NULL, " +
                        "`PROPERTY_ID` BIGINT NOT NULL, " +
                        "`KEY` VARCHAR(255) NOT NULL, " +
                        "`VALUE` VARCHAR(1024), " +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES PROPERTIES(ID) ON DELETE CASCADE)");
                connection.commit();
            }
        } catch (SQLException e) {
            throw new SQLException(CREATE_CONFIG_TABLE_ERROR, e);
        }
    }

    private static final class JDBCUtils {

        private static Connection open(final DataSource dataSource) throws SQLException {
            final Connection connection = Objects.requireNonNull(dataSource).getConnection();
            connection.setAutoCommit(false);
            return connection;
        }

        private static void rollback(final Connection connection, final SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException(DB_ROLLBACK_ERROR, e);
                }
            }
        }

        private static void close(final Connection connection) {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(DB_CONNECTION_ERROR);
                }
            }
        }

        private static void setParameters(final PreparedStatement statement, final Config config) throws SQLException {
            statement.setString(1, config.getName());
            statement.setString(2, config.getDescription());
            statement.setLong(3, config.getVersion());
            statement.setLong(4, config.getUpdated());
        }

        private static void setParameters(final PreparedStatement statement, final String[] names) throws SQLException {
            for (int i = 0; i < names.length; i++) {
                statement.setString(i + 1, names[i]);
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
        Builder(final DataSource dataSource) {
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
