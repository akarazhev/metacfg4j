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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_CONFIG_TABLE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_UTILS_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_CONNECTION_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ROLLBACK_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_ATTRIBUTES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_CONFIG_PROPERTIES_ERROR;
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
        // TODO: 3. refactor it
        this.dataSource = builder.dataSource;
        init(this.dataSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(final Stream<String> stream) {
        try {
            final StringBuilder sql = new StringBuilder("SELECT C.ID, C.NAME, C.DESCRIPTION, C.VERSION, C.UPDATED, A.KEY, " +
                    "A.VALUE FROM CONFIGS AS C LEFT JOIN CONFIG_ATTRIBUTES AS A ON C.ID = A.CONFIG_ID WHERE C.NAME = ?");
            final String[] names = stream.toArray(String[]::new);
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> sql.append(" OR C.NAME = ?"));
            }

            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                JDBCUtils.setStatement(statement, names);

                try (final ResultSet resultSet = statement.executeQuery()) {
                    final Map<Integer, Config> configs = new HashMap<>();
                    while (resultSet.next()) {
                        final int id = resultSet.getInt(1);
                        final Config config = configs.get(id);
                        if (config == null) {
                            configs.put(id, new Config.Builder(resultSet.getString(2), Collections.emptyList()).
                                    id(id).
                                    description(resultSet.getString(3)).
                                    version(resultSet.getInt(4)).
                                    updated(resultSet.getLong(5)).
                                    attribute(resultSet.getString(6), resultSet.getString(7)).
                                    build());
                        } else {
                            configs.put(id, new Config.Builder(config).
                                    attribute(resultSet.getString(6), resultSet.getString(7)).
                                    build());
                        }
                    }

                    return configs.values().stream();
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
            try (final Connection connection = dataSource.getConnection();
                 final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(SQL.SELECT.NAMES)) {
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
        final List<Config> toUpdate = new LinkedList<>();
        final List<Config> toInsert = new LinkedList<>();
        stream.forEach(config -> {
            if (config.getId() > 1) {
                toUpdate.add(config);
            } else {
                toInsert.add(config);
            }
        });

        return Stream.concat(update(connection, toUpdate.toArray(new Config[0])),
                insert(connection, toInsert.toArray(new Config[0])));
    }

    private Stream<Config> insert(final Connection connection, final Config[] configs) throws SQLException {
        final Config[] inserted = new Config[configs.length];
        try (final PreparedStatement statement =
                     connection.prepareStatement(SQL.INSERT.CONFIGS, Statement.RETURN_GENERATED_KEYS)) {
            for (final Config config : configs) {
                JDBCUtils.setStatement(statement, config);
                statement.addBatch();
            }

            if (statement.executeBatch().length == configs.length) {
                try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                    final Set<SQLException> exceptions = new HashSet<>();
                    for (int i = 0; i < configs.length; i++) {
                        resultSet.absolute(i + 1);
                        final int configId = resultSet.getInt(1);
                        final Config config = new Config.Builder(configs[i]).id(configId).build();
                        // Create config attributes
                        config.getAttributes().ifPresent(map -> {
                            try {
                                insertAttributes(connection, SQL.INSERT.CONFIG_ATTRIBUTES, configId, map);
                            } catch (SQLException e) {
                                exceptions.add(e);
                            }
                        });
                        // Create config properties
                        insert(connection, configId, config.getProperties().toArray(Property[]::new));
                        inserted[i] = config;
                    }

                    if (exceptions.size() > 0) {
                        throw new SQLException(INSERT_ATTRIBUTES_ERROR);
                    }
                }
            } else {
                throw new SQLException(INSERT_CONFIGS_ERROR);
            }

            connection.commit();
        }

        return Arrays.stream(inserted);
    }

    private void insertAttributes(final Connection connection, final String sql, final int id,
                                  final Map<String, String> map) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            JDBCUtils.setBatchStatement(statement, id, map);

            if (statement.executeBatch().length != map.size()) {
                throw new SQLException(INSERT_ATTRIBUTES_ERROR);
            }
        }
    }

    private void insert(final Connection connection, final int configId, final Property[] properties)
            throws SQLException {
        try (final PreparedStatement statement =
                     connection.prepareStatement(SQL.INSERT.PROPERTIES, Statement.RETURN_GENERATED_KEYS)) {
            for (final Property property : properties) {
                JDBCUtils.setBatchStatement(statement, configId, property);
            }

            insert(connection, statement, configId, properties);
        }
    }

    private void insert(final Connection connection, final int configId, final int propertyId,
                        final Property[] properties) throws SQLException {
        try (final PreparedStatement statement =
                     connection.prepareStatement(SQL.INSERT.SUB_PROPERTIES, Statement.RETURN_GENERATED_KEYS)) {
            for (final Property property : properties) {
                JDBCUtils.setBatchStatement(statement, configId, propertyId, property);
            }

            insert(connection, statement, configId, properties);
        }
    }

    private void insert(final Connection connection, final PreparedStatement statement, final int configId,
                        final Property[] properties) throws SQLException {
        if (statement.executeBatch().length == properties.length) {
            try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                final Set<SQLException> exceptions = new HashSet<>();
                for (int i = 0; i < properties.length; i++) {
                    resultSet.absolute(i + 1);
                    final int propertyId = resultSet.getInt(1);
                    // Create property attributes
                    properties[i].getAttributes().ifPresent(map -> {

                        try {
                            insertAttributes(connection, SQL.INSERT.PROPERTY_ATTRIBUTES, propertyId, map);
                        } catch (SQLException e) {
                            exceptions.add(e);
                        }
                    });
                    // Create property properties
                    insert(connection, configId, propertyId, properties[i].getProperties().toArray(Property[]::new));
                }

                if (exceptions.size() > 0) {
                    throw new SQLException(INSERT_ATTRIBUTES_ERROR);
                }
            }
        } else {
            throw new SQLException(INSERT_CONFIG_PROPERTIES_ERROR);
        }
    }

    private Stream<Config> update(final Connection connection, final Config[] configs) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement(SQL.UPDATE.CONFIGS)) {
            for (final Config config : configs) {
                statement.setLong(5, config.getId());
                JDBCUtils.setStatement(statement, config);
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
                JDBCUtils.setStatement(statement, names);
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
                statement.executeUpdate(SQL.CREATE_TABLE.CONFIGS);
                statement.executeUpdate(SQL.CREATE_TABLE.CONFIG_ATTRIBUTES);
                statement.executeUpdate(SQL.CREATE_TABLE.PROPERTIES);
                statement.executeUpdate(SQL.CREATE_TABLE.PROPERTY_ATTRIBUTES);
                connection.commit();
            }
        } catch (SQLException e) {
            throw new SQLException(CREATE_CONFIG_TABLE_ERROR, e);
        }
    }

    private final static class SQL {

        private SQL() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        final static class INSERT {

            private INSERT() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS = "INSERT INTO `CONFIGS` (`NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) " +
                    "VALUES (?, ?, ?, ?);";
            static final String CONFIG_ATTRIBUTES = "INSERT INTO `CONFIG_ATTRIBUTES` (`CONFIG_ID`, `KEY`, `VALUE`) " +
                    "VALUES (?, ?, ?);";
            static final String PROPERTIES = "INSERT INTO `PROPERTIES` (`CONFIG_ID`, `NAME`, `CAPTION`, " +
                    "`DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES (?, ?, ?, ?, ?, ?, ?);";
            static final String PROPERTY_ATTRIBUTES = "INSERT INTO `PROPERTY_ATTRIBUTES` (`PROPERTY_ID`, `KEY`, " +
                    "`VALUE`) VALUES (?, ?, ?);";
            static final String SUB_PROPERTIES = "INSERT INTO `PROPERTIES` (`PROPERTY_ID`, `CONFIG_ID`, `NAME`, " +
                    "`CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        }

        final static class UPDATE {

            private UPDATE() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS = "UPDATE `CONFIGS` SET `NAME` = ?, `DESCRIPTION` = ?, `VERSION` = ?, " +
                    "`UPDATED` = ? WHERE `ID` = ?;";
        }

        final static class SELECT {

            private SELECT() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String NAMES = "SELECT `NAME` FROM `CONFIGS`;";
        }

        final static class CREATE_TABLE {

            private CREATE_TABLE() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS =
                    "CREATE TABLE IF NOT EXISTS `CONFIGS` " +
                    "(`ID` IDENTITY NOT NULL, " +
                    "`NAME` VARCHAR(255) NOT NULL, " +
                    "`DESCRIPTION` VARCHAR(1024), " +
                    "`VERSION` INT NOT NULL, " +
                    "`UPDATED` BIGINT NOT NULL);";
            static final String CONFIG_ATTRIBUTES =
                    "CREATE TABLE IF NOT EXISTS `CONFIG_ATTRIBUTES` " +
                    "(`ID` IDENTITY NOT NULL, " +
                    "`CONFIG_ID` BIGINT NOT NULL, " +
                    "`KEY` VARCHAR(255) NOT NULL, " +
                    "`VALUE` VARCHAR(1024), " +
                    "FOREIGN KEY(CONFIG_ID) REFERENCES CONFIGS(ID) ON DELETE CASCADE)";
            static final String PROPERTIES =
                    "CREATE TABLE IF NOT EXISTS `PROPERTIES` " +
                    "(`ID` IDENTITY NOT NULL, " +
                    "`PROPERTY_ID` BIGINT, " +
                    "`CONFIG_ID` BIGINT NOT NULL, " +
                    "`NAME` VARCHAR(255) NOT NULL, " +
                    "`CAPTION` VARCHAR(255), " +
                    "`DESCRIPTION` VARCHAR(1024), " +
                    "`TYPE` ENUM ('BOOL', 'DOUBLE', 'LONG', 'STRING', 'STRING_ARRAY') NOT NULL, " +
                    "`VALUE` VARCHAR(4096) NOT NULL, " +
                    "`VERSION` INT NOT NULL, " +
                    "FOREIGN KEY(CONFIG_ID) REFERENCES CONFIGS(ID) ON DELETE CASCADE," +
                    "FOREIGN KEY(PROPERTY_ID) REFERENCES PROPERTIES(ID) ON DELETE CASCADE)";
            static final String PROPERTY_ATTRIBUTES =
                    "CREATE TABLE IF NOT EXISTS `PROPERTY_ATTRIBUTES` " +
                    "(`ID` IDENTITY NOT NULL, " +
                    "`PROPERTY_ID` BIGINT NOT NULL, " +
                    "`KEY` VARCHAR(255) NOT NULL, " +
                    "`VALUE` VARCHAR(1024), " +
                    "FOREIGN KEY(PROPERTY_ID) REFERENCES PROPERTIES(ID) ON DELETE CASCADE)";
        }
    }

    private static final class JDBCUtils {

        private JDBCUtils() {
            throw new AssertionError(CREATE_UTILS_CLASS_ERROR);
        }

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

        private static void setStatement(final PreparedStatement statement, final Config config) throws SQLException {
            statement.setString(1, config.getName());
            statement.setString(2, config.getDescription());
            statement.setLong(3, config.getVersion());
            statement.setLong(4, config.getUpdated());
        }

        private static void setStatement(final PreparedStatement statement, final String[] names) throws SQLException {
            for (int i = 0; i < names.length; i++) {
                statement.setString(i + 1, names[i]);
            }
        }

        private static void setBatchStatement(final PreparedStatement statement, final int id,
                                              final Map<String, String> map) throws SQLException {
            for (final String key : map.keySet()) {
                statement.setInt(1, id);
                statement.setString(2, key);
                statement.setString(3, map.get(key));
                statement.addBatch();
            }
        }

        private static void setBatchStatement(final PreparedStatement statement, final int configId,
                                              final Property property) throws SQLException {
            statement.setInt(1, configId);
            statement.setString(2, property.getName());
            if (property.getCaption().isPresent()) {
                statement.setString(3, property.getCaption().get());
            } else {
                statement.setString(3, null);
            }

            if (property.getDescription().isPresent()) {
                statement.setString(4, property.getDescription().get());
            } else {
                statement.setString(4, null);
            }

            statement.setString(5, property.getType());
            statement.setString(6, property.getValue());
            statement.setInt(7, property.getVersion());
            statement.addBatch();
        }

        private static void setBatchStatement(final PreparedStatement statement, final int configId,
                                              final int propertyId, final Property property) throws SQLException {
            statement.setInt(1, propertyId);
            statement.setInt(2, configId);
            statement.setString(3, property.getName());
            if (property.getCaption().isPresent()) {
                statement.setString(4, property.getCaption().get());
            } else {
                statement.setString(4, null);
            }

            if (property.getDescription().isPresent()) {
                statement.setString(5, property.getDescription().get());
            } else {
                statement.setString(5, null);
            }

            statement.setString(6, property.getType());
            statement.setString(7, property.getValue());
            statement.setInt(8, property.getVersion());
            statement.addBatch();
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
