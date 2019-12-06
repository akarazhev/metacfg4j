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

import com.github.akarazhev.metaconfig.extension.Validator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_CONFIGS_ERROR;
import static java.util.AbstractMap.SimpleEntry;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_ATTRIBUTES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_CONFIG_TABLE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_UTILS_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_CONNECTION_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ROLLBACK_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_ATTRIBUTES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_CONFIG_PROPERTIES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;

/**
 * {@inheritDoc}
 */
final class DbConfigRepository implements ConfigRepository {
    private final DataSource dataSource;
    private final Map<String, String> mapping;

    private DbConfigRepository(final Builder builder) {
        this.dataSource = builder.dataSource;
        this.mapping = JDBCUtils.createMapping(builder.mapping);
        JDBCUtils.createDataBase(this.dataSource, this.mapping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(final Stream<String> stream) {
        final String[] names = stream.toArray(String[]::new);
        if (names.length > 0) {
            try {
                final String sql = String.format(SQL.SELECT.CONFIGS, mapping.get(SQL.MAPPING.CONFIGS),
                        mapping.get(SQL.MAPPING.CONFIG_ATTRIBUTES), mapping.get(SQL.MAPPING.PROPERTIES),
                        mapping.get(SQL.MAPPING.PROPERTY_ATTRIBUTES));
                try (final Connection connection = dataSource.getConnection();
                     final PreparedStatement statement = connection.prepareStatement(JDBCUtils.getSql(sql, names))) {
                    JDBCUtils.setStatement(statement, names);

                    try (final ResultSet resultSet = statement.executeQuery()) {
                        int prevConfigId = -1;
                        final Map<Integer, Config> configs = new HashMap<>();
                        final Map<Integer, Property> properties = new HashMap<>();
                        final Collection<SimpleEntry<Integer, Integer>> links = new LinkedList<>();
                        while (resultSet.next()) {
                            // Create properties
                            final int propertyId = resultSet.getInt(8);
                            final Property property = properties.get(propertyId);
                            if (property == null) {
                                properties.put(propertyId, new Property.Builder(resultSet.getString(10),
                                        resultSet.getString(13),
                                        resultSet.getString(14)).
                                        caption(resultSet.getString(11)).
                                        description(resultSet.getString(12)).
                                        attribute(resultSet.getString(15), resultSet.getString(16)).
                                        build());
                            } else {
                                properties.put(propertyId, new Property.Builder(property).
                                        attribute(resultSet.getString(15), resultSet.getString(16)).
                                        build());
                            }
                            // Create links
                            links.add(new SimpleEntry<>(propertyId, resultSet.getInt(9)));
                            // Create configs
                            final int configId = resultSet.getInt(1);
                            final Config config = configs.get(configId);
                            if (config == null) {
                                configs.put(configId,
                                        new Config.Builder(resultSet.getString(2), Collections.emptyList()).
                                                id(configId).
                                                description(resultSet.getString(3)).
                                                version(resultSet.getInt(4)).
                                                updated(resultSet.getLong(5)).
                                                attribute(resultSet.getString(6),
                                                        resultSet.getString(7)).
                                                build());
                            } else {
                                configs.put(configId, new Config.Builder(config).
                                        attribute(resultSet.getString(6), resultSet.getString(7)).
                                        build());
                            }
                            // Set properties to the config
                            if (prevConfigId > -1 && configId != prevConfigId) {
                                configs.put(prevConfigId, new Config.Builder(configs.get(prevConfigId)).
                                        properties(new String[0], getLinkedProps(properties, links)).build());
                                links.clear();
                                properties.clear();
                            }

                            prevConfigId = configId;
                        }

                        if (configs.size() > 0) {
                            configs.put(prevConfigId, new Config.Builder(configs.get(prevConfigId)).
                                    properties(new String[0], getLinkedProps(properties, links)).build());
                        }

                        return configs.values().stream();
                    }
                }
            } catch (final SQLException e) {
                throw new RuntimeException(RECEIVED_CONFIGS_ERROR, e);
            }
        }

        return Stream.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        try {
            final String sql = String.format(SQL.SELECT.NAMES, mapping.get(SQL.MAPPING.CONFIGS));
            try (final Connection connection = dataSource.getConnection();
                 final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                final Collection<String> names = new LinkedList<>();
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
            return Arrays.stream(saveAndFlush(connection, stream.toArray(Config[]::new)));
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
            return delete(connection, stream.toArray(String[]::new));
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }

        return 0;
    }

    private Collection<Property> getLinkedProps(final Map<Integer, Property> properties,
                                                final Collection<SimpleEntry<Integer, Integer>> links) {
        final Comparator<SimpleEntry<Integer, Integer>> comparing =
                Comparator.comparing(SimpleEntry::getValue);
        final Map<Integer, Property> linkedProps = new HashMap<>(properties);
        final Collection<SimpleEntry<Integer, Integer>> sortedLinks = new ArrayList<>(links);
        sortedLinks.stream().
                sorted(comparing.reversed()).
                forEach(link -> {
                    final Property childProp = linkedProps.get(link.getKey());
                    final Property parentProp = linkedProps.get(link.getValue());
                    if (childProp != null && parentProp != null) {
                        linkedProps.put(link.getValue(),
                                new Property.Builder(parentProp).property(new String[0], childProp).build());
                        linkedProps.remove(link.getKey());
                    }
                });

        return linkedProps.values();
    }

    private Config[] saveAndFlush(final Connection connection, final Config[] configs) throws SQLException {
        final Collection<Config> toUpdate = new LinkedList<>();
        final Collection<Config> toInsert = new LinkedList<>();
        for (Config config : configs) {
            if (config.getId() > 0) {
                toUpdate.add(config);
            } else {
                toInsert.add(config);
            }
        }

        final Config[] savedConfigs = new Config[toUpdate.size() + toInsert.size()];
        if (toUpdate.size() > 0) {
            final Config[] updated = update(connection, toUpdate.toArray(new Config[0]));
            System.arraycopy(updated, 0, savedConfigs, 0, updated.length);
        }

        if (toInsert.size() > 0) {
            final int pos = toUpdate.size() == 0 ? 0 : toUpdate.size() + 1;
            final Config[] inserted = insert(connection, toInsert.toArray(new Config[0]));
            System.arraycopy(inserted, 0, savedConfigs, pos, inserted.length);
        }

        return savedConfigs;
    }

    private Config[] insert(final Connection connection, final Config[] configs) throws SQLException {
        if (configs.length > 0) {
            final Config[] inserted = new Config[configs.length];
            final String configsSql = String.format(SQL.INSERT.CONFIGS, mapping.get(SQL.MAPPING.CONFIGS));
            try (final PreparedStatement statement =
                         connection.prepareStatement(configsSql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Config config : configs) {
                    JDBCUtils.setStatement(statement, config, 1);
                    statement.addBatch();
                }

                if (statement.executeBatch().length == configs.length) {
                    try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                        final Collection<Throwable> exceptions = new LinkedList<>();
                        for (int i = 0; i < configs.length; i++) {
                            resultSet.absolute(i + 1);
                            final int configId = resultSet.getInt(1);
                            final Config config = new Config.Builder(configs[i]).id(configId).build();
                            final Property[] properties = config.getProperties().toArray(Property[]::new);
                            // Create config attributes
                            config.getAttributes().ifPresent(map -> {
                                try {
                                    final String attributesSql = String.format(SQL.INSERT.CONFIG_ATTRIBUTES,
                                            mapping.get(SQL.MAPPING.CONFIG_ATTRIBUTES));
                                    insert(connection, attributesSql, configId, map);
                                } catch (final SQLException e) {
                                    exceptions.add(e);
                                }
                            });
                            // Create config properties
                            insert(connection, configId, properties);
                            inserted[i] = config;
                        }

                        if (exceptions.size() > 0) {
                            throw new SQLException(INSERT_ATTRIBUTES_ERROR);
                        }
                    }
                } else {
                    throw new SQLException(SAVE_CONFIGS_ERROR);
                }

                connection.commit();
            }

            return inserted;
        }

        return new Config[0];
    }

    private void insert(final Connection connection, final String sql, final int id, final Map<String, String> map)
            throws SQLException {
        if (map.size() > 0) {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                JDBCUtils.setBatchStatement(statement, id, map);

                if (statement.executeBatch().length != map.size()) {
                    throw new SQLException(INSERT_ATTRIBUTES_ERROR);
                }
            }
        }
    }

    private void insert(final Connection connection, final int configId, final Property[] properties)
            throws SQLException {
        if (properties.length > 0) {
            final String sql = String.format(SQL.INSERT.PROPERTIES, mapping.get(SQL.MAPPING.PROPERTIES));
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Property property : properties) {
                    JDBCUtils.setBatchStatement(statement, configId, property);
                }

                insert(connection, statement, configId, properties);
            }
        }
    }

    private void insert(final Connection connection, final SimpleEntry<Integer, Integer> confPropIds,
                        final Property[] properties) throws SQLException {
        if (properties.length > 0) {
            final String sql = String.format(SQL.INSERT.SUB_PROPERTIES, mapping.get(SQL.MAPPING.PROPERTIES));
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Property property : properties) {
                    JDBCUtils.setBatchStatement(statement, confPropIds, property);
                }

                insert(connection, statement, confPropIds.getKey(), properties);
            }
        }
    }

    private void insert(final Connection connection, final PreparedStatement statement, final int configId,
                        final Property[] properties) throws SQLException {
        if (statement.executeBatch().length == properties.length) {
            try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (int i = 0; i < properties.length; i++) {
                    resultSet.absolute(i + 1);
                    final int propertyId = resultSet.getInt(1);
                    // Create property attributes
                    properties[i].getAttributes().ifPresent(map -> {
                        try {
                            final String sql = String.format(SQL.INSERT.PROPERTY_ATTRIBUTES,
                                    mapping.get(SQL.MAPPING.PROPERTY_ATTRIBUTES));
                            insert(connection, sql, propertyId, map);
                        } catch (final SQLException e) {
                            exceptions.add(e);
                        }
                    });
                    // Create property properties
                    insert(connection, new SimpleEntry<>(configId, propertyId),
                            properties[i].getProperties().toArray(Property[]::new));
                }

                if (exceptions.size() > 0) {
                    throw new SQLException(INSERT_ATTRIBUTES_ERROR);
                }
            }
        } else {
            throw new SQLException(INSERT_CONFIG_PROPERTIES_ERROR);
        }
    }

    private Config[] update(final Connection connection, final Config[] configs) throws SQLException {
        if (configs.length > 0) {
            final Config[] updated = new Config[configs.length];
            final String sql = String.format(SQL.UPDATE.CONFIGS, mapping.get(SQL.MAPPING.CONFIGS));
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                final Map<Integer, Integer> idVersion = new HashMap<>();
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (final Config config : configs) {
                    final int version = getVersion(connection, config.getId()) + 1;
                    statement.setInt(5, config.getId());
                    statement.setInt(6, config.getVersion());
                    JDBCUtils.setStatement(statement, config, version);
                    statement.addBatch();
                    // Update config attributes
                    final Property[] properties = config.getProperties().toArray(Property[]::new);
                    config.getAttributes().ifPresent(map -> {
                        try {
                            final String name = mapping.get(SQL.MAPPING.CONFIG_ATTRIBUTES);
                            delete(connection, String.format(SQL.DELETE.CONFIG_ATTRIBUTES, name), config.getId());
                            insert(connection, String.format(SQL.INSERT.CONFIG_ATTRIBUTES, name), config.getId(), map);
                        } catch (final SQLException e) {
                            exceptions.add(e);
                        }
                    });
                    // Update config properties
                    delete(connection, String.format(SQL.DELETE.PROPERTIES, mapping.get(SQL.MAPPING.PROPERTIES)),
                            config.getId());
                    insert(connection, config.getId(), properties);
                    idVersion.put(config.getId(), version);
                }

                if (statement.executeBatch().length == configs.length) {
                    if (exceptions.size() > 0) {
                        throw new SQLException(UPDATE_ATTRIBUTES_ERROR);
                    }

                    if (statement.getUpdateCount() == 0) {
                        throw new SQLException(SAVE_CONFIGS_ERROR);
                    }

                    for (int i = 0; i < configs.length; i++) {
                        updated[i] = new Config.Builder(configs[i]).
                                version(idVersion.get(configs[i].getId())).
                                build();
                    }
                } else {
                    throw new SQLException(SAVE_CONFIGS_ERROR);
                }

                connection.commit();
            }

            return updated;
        }

        return new Config[0];
    }

    private int getVersion(final Connection connection, final int id) throws SQLException {
        final String sql = String.format(SQL.SELECT.VERSION, SQL.MAPPING.CONFIGS);
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        return 0;
    }

    private int delete(final Connection connection, final String[] names) throws SQLException {
        if (names.length > 0) {
            try {
                final String sql = String.format(SQL.DELETE.CONFIGS, mapping.get(SQL.MAPPING.CONFIGS));
                try (final PreparedStatement statement = connection.prepareStatement(JDBCUtils.getSql(sql, names))) {
                    JDBCUtils.setStatement(statement, names);
                    final int deleted = statement.executeUpdate();
                    connection.commit();
                    return deleted;
                }
            } catch (final SQLException e) {
                throw new SQLException(DELETE_CONFIGS_ERROR, e);
            }
        }

        return 0;
    }

    private void delete(final Connection connection, final String sql, final int id) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
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

            static final String CONFIGS =
                    "INSERT INTO `%s` (`NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (?, ?, ?, ?);";
            static final String CONFIG_ATTRIBUTES =
                    "INSERT INTO `%s` (`CONFIG_ID`, `KEY`, `VALUE`) VALUES (?, ?, ?);";
            static final String PROPERTIES =
                    "INSERT INTO `%s` (`CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`) " +
                            "VALUES (?, ?, ?, ?, ?, ?);";
            static final String PROPERTY_ATTRIBUTES =
                    "INSERT INTO `%s` (`PROPERTY_ID`, `KEY`, `VALUE`) VALUES (?, ?, ?);";
            static final String SUB_PROPERTIES =
                    "INSERT INTO `%s` (`PROPERTY_ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?);";
        }

        final static class UPDATE {

            private UPDATE() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS =
                    "UPDATE `%s` SET `NAME` = ?, `DESCRIPTION` = ?, `VERSION` = ?, `UPDATED` = ? " +
                            "WHERE `ID` = ? AND `VERSION` = ?;";
        }

        final static class DELETE {

            private DELETE() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS =
                    "DELETE FROM `%s` AS `C` WHERE `C`.`NAME` = ?";
            static final String CONFIG_ATTRIBUTES =
                    "DELETE FROM `%s` AS `CA` WHERE `CA`.`CONFIG_ID` = ?;";
            static final String PROPERTIES =
                    "DELETE FROM `%s` AS `P` WHERE `P`.`CONFIG_ID` = ?;";
        }

        final static class SELECT {

            private SELECT() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String NAMES = "SELECT `NAME` FROM `%s` ORDER BY `NAME`;";
            static final String VERSION = "SELECT `VERSION` FROM `%s` WHERE `ID` = ?;";
            static final String CONFIGS =
                    "SELECT `C`.`ID`, `C`.`NAME`, `C`.`DESCRIPTION`, `C`.`VERSION`, `C`.`UPDATED`, `CA`.`KEY`, " +
                            "`CA`.`VALUE`, `P`.`ID`, `P`.`PROPERTY_ID`, `P`.`NAME` , `P`.`CAPTION`, " +
                            "`P`.`DESCRIPTION`, `P`.`TYPE`, `P`.`VALUE`, `PA`.`KEY`, `PA`.`VALUE` " +
                            "FROM `%1$s` AS `C` " +
                            "LEFT JOIN `%3$s` AS `P` ON `C`.`ID` = `P`.`CONFIG_ID` " +
                            "LEFT JOIN `%2$s` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID` " +
                            "LEFT JOIN `%4$s` AS `PA` ON `P`.`ID` = `PA`.`PROPERTY_ID` " +
                            "WHERE `C`.`NAME` = ?";
        }

        final static class MAPPING {

            private MAPPING() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS = "configs";
            static final String CONFIG_ATTRIBUTES = "config-attributes";
            static final String PROPERTIES = "properties";
            static final String PROPERTY_ATTRIBUTES = "property-attributes";
        }

        final static class CREATE_TABLE {

            private CREATE_TABLE() {
                throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
            }

            static final String CONFIGS =
                    "CREATE TABLE IF NOT EXISTS `%s` " +
                            "(`ID` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                            "`NAME` VARCHAR(255) NOT NULL, " +
                            "`DESCRIPTION` VARCHAR(1024), " +
                            "`VERSION` INT NOT NULL, " +
                            "`UPDATED` BIGINT NOT NULL);";
            static final String CONFIG_ATTRIBUTES =
                    "CREATE TABLE IF NOT EXISTS `%2$s` " +
                            "(`ID` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                            "`CONFIG_ID` BIGINT NOT NULL, " +
                            "`KEY` VARCHAR(255) NOT NULL, " +
                            "`VALUE` VARCHAR(1024), " +
                            "FOREIGN KEY(CONFIG_ID) REFERENCES %1$s(ID) ON DELETE CASCADE)";
            static final String PROPERTIES =
                    "CREATE TABLE IF NOT EXISTS `%1$s` " +
                            "(`ID` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                            "`PROPERTY_ID` BIGINT, " +
                            "`CONFIG_ID` BIGINT NOT NULL, " +
                            "`NAME` VARCHAR(255) NOT NULL, " +
                            "`CAPTION` VARCHAR(255), " +
                            "`DESCRIPTION` VARCHAR(1024), " +
                            "`TYPE` ENUM ('BOOL', 'DOUBLE', 'LONG', 'STRING', 'STRING_ARRAY') NOT NULL, " +
                            "`VALUE` VARCHAR(4096) NOT NULL, " +
                            "FOREIGN KEY(CONFIG_ID) REFERENCES %2$s(ID) ON DELETE CASCADE," +
                            "FOREIGN KEY(PROPERTY_ID) REFERENCES %1$s(ID) ON DELETE CASCADE)";
            static final String PROPERTY_ATTRIBUTES =
                    "CREATE TABLE IF NOT EXISTS `%1$s` " +
                            "(`ID` BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                            "`PROPERTY_ID` BIGINT NOT NULL, " +
                            "`KEY` VARCHAR(255) NOT NULL, " +
                            "`VALUE` VARCHAR(1024), " +
                            "FOREIGN KEY(PROPERTY_ID) REFERENCES %2$s(ID) ON DELETE CASCADE)";
        }
    }

    private static final class JDBCUtils {

        private JDBCUtils() {
            throw new AssertionError(CREATE_UTILS_CLASS_ERROR);
        }

        private static void createDataBase(final DataSource dataSource, final Map<String, String> mapping) {
            Connection connection = null;
            try {
                connection = JDBCUtils.open(dataSource);
                createTables(connection, mapping);
            } catch (final SQLException e) {
                JDBCUtils.rollback(connection, e);
            } finally {
                JDBCUtils.close(connection);
            }
        }

        private static void createTables(final Connection connection, final Map<String, String> mapping) throws SQLException {
            try {
                try (final Statement statement = connection.createStatement()) {
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.CONFIGS, mapping.get(SQL.MAPPING.CONFIGS)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.CONFIG_ATTRIBUTES,
                            mapping.get(SQL.MAPPING.CONFIGS), mapping.get(SQL.MAPPING.CONFIG_ATTRIBUTES)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.PROPERTIES,
                            mapping.get(SQL.MAPPING.PROPERTIES), mapping.get(SQL.MAPPING.CONFIGS)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.PROPERTY_ATTRIBUTES,
                            mapping.get(SQL.MAPPING.PROPERTY_ATTRIBUTES), mapping.get(SQL.MAPPING.PROPERTIES)));
                    connection.commit();
                }
            } catch (final SQLException e) {
                throw new SQLException(CREATE_CONFIG_TABLE_ERROR, e);
            }
        }

        private static Map<String, String> createMapping(final Map<String, String> mapping) {
            if (mapping == null) {
                final Map<String, String> defaultMapping = new HashMap<>();
                defaultMapping.put(SQL.MAPPING.CONFIGS, "CONFIGS");
                defaultMapping.put(SQL.MAPPING.CONFIG_ATTRIBUTES, "CONFIG_ATTRIBUTES");
                defaultMapping.put(SQL.MAPPING.PROPERTIES, "PROPERTIES");
                defaultMapping.put(SQL.MAPPING.PROPERTY_ATTRIBUTES, "PROPERTY_ATTRIBUTES");
                return defaultMapping;
            } else {
                mapping.putIfAbsent(SQL.MAPPING.CONFIGS, "CONFIGS");
                mapping.putIfAbsent(SQL.MAPPING.CONFIG_ATTRIBUTES, "CONFIG_ATTRIBUTES");
                mapping.putIfAbsent(SQL.MAPPING.PROPERTIES, "PROPERTIES");
                mapping.putIfAbsent(SQL.MAPPING.PROPERTY_ATTRIBUTES, "PROPERTY_ATTRIBUTES");
            }

            return mapping;
        }

        private static String getSql(final String query, final String[] names) {
            final StringBuilder sql = new StringBuilder(query);
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> sql.append(" OR C.NAME = ?"));
            }

            return sql.append(";").toString();
        }

        private static Connection open(final DataSource dataSource) throws SQLException {
            final Connection connection = Validator.of(dataSource).get().getConnection();
            connection.setAutoCommit(false);
            return connection;
        }

        private static void rollback(final Connection connection, final SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (final SQLException ex) {
                    throw new RuntimeException(DB_ROLLBACK_ERROR, e);
                }

                throw new RuntimeException(DB_ERROR, e);
            }
        }

        private static void close(final Connection connection) {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (final SQLException e) {
                    throw new RuntimeException(DB_CONNECTION_ERROR);
                }
            }
        }

        private static void setStatement(final PreparedStatement statement, final Config config, final int version)
                throws SQLException {
            statement.setString(1, config.getName());
            if (config.getDescription().isPresent()) {
                statement.setString(2, config.getDescription().get());
            } else {
                statement.setString(2, null);
            }
            statement.setInt(3, version);
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

            statement.setString(5, property.getType().name());
            statement.setString(6, property.getValue());
            statement.addBatch();
        }

        private static void setBatchStatement(final PreparedStatement statement,
                                              final SimpleEntry<Integer, Integer> confPropIds,
                                              final Property property) throws SQLException {
            statement.setInt(1, confPropIds.getValue());
            statement.setInt(2, confPropIds.getKey());
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

            statement.setString(6, property.getType().name());
            statement.setString(7, property.getValue());
            statement.addBatch();
        }
    }

    /**
     * Wraps and builds the instance of the DB config repository.
     */
    public final static class Builder {
        private final DataSource dataSource;
        private Map<String, String> mapping;

        /**
         * Constructs a DB config repository with a required parameter.
         *
         * @param dataSource a datasource.
         */
        Builder(final DataSource dataSource) {
            this.dataSource = Validator.of(dataSource).get();
        }

        /**
         * Constructs a DB config repository with a mapping.
         *
         * @param mapping a table mapping.
         * @return a builder of the DB config repository.
         */
        public Builder mapping(final Map<String, String> mapping) {
            this.mapping = Validator.of(mapping).
                    validate(m -> {
                        if (m.containsKey(SQL.MAPPING.CONFIGS)) {
                            final String configs = m.get(SQL.MAPPING.CONFIGS);
                            return configs != null && configs.length() > 0;
                        }

                        return true;
                    }, SQL.MAPPING.CONFIGS + " mapping is wrong.").
                    validate(m -> {
                        if (m.containsKey(SQL.MAPPING.CONFIG_ATTRIBUTES)) {
                            final String configs = m.get(SQL.MAPPING.CONFIG_ATTRIBUTES);
                            return configs != null && configs.length() > 0;
                        }

                        return true;
                    }, SQL.MAPPING.CONFIG_ATTRIBUTES + " mapping is wrong.").
                    validate(m -> {
                        if (m.containsKey(SQL.MAPPING.PROPERTIES)) {
                            final String configs = m.get(SQL.MAPPING.PROPERTIES);
                            return configs != null && configs.length() > 0;
                        }

                        return true;
                    }, SQL.MAPPING.PROPERTIES + " mapping is wrong.").
                    validate(m -> {
                        if (m.containsKey(SQL.MAPPING.PROPERTY_ATTRIBUTES)) {
                            final String configs = m.get(SQL.MAPPING.PROPERTY_ATTRIBUTES);
                            return configs != null && configs.length() > 0;
                        }

                        return true;
                    }, SQL.MAPPING.PROPERTY_ATTRIBUTES + " mapping is wrong.").get();
            return this;
        }

        /**
         * Builds a DB config repository with a required parameter.
         *
         * @return a builder of the DB config repository.
         */
        public ConfigRepository build() {
            return new DbConfigRepository(this);
        }
    }
}
