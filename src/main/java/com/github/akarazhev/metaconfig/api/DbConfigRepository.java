/* Copyright 2019-2020 Andrey Karazhev
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Mapping.CONFIGS_TABLE;
import static com.github.akarazhev.metaconfig.Constants.Mapping.CONFIG_ATTRIBUTES_TABLE;
import static com.github.akarazhev.metaconfig.Constants.Mapping.PROPERTIES_TABLE;
import static com.github.akarazhev.metaconfig.Constants.Mapping.PROPERTY_ATTRIBUTES_TABLE;
import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_CONFIG_TABLE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_CONNECTION_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DB_ROLLBACK_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_ATTRIBUTES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIG_NAMES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_PAGE_RESPONSE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_PROPERTIES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_ATTRIBUTES_ERROR;
import static java.util.AbstractMap.SimpleEntry;

/**
 * {@inheritDoc}
 */
final class DbConfigRepository implements ConfigRepository {
    private final DataSource dataSource;
    private final Map<String, String> mapping = new HashMap<>();

    private DbConfigRepository(final Builder builder) {
        this.dataSource = builder.dataSource;
        this.mapping.putAll(JDBCUtils.createMapping(builder.mapping));
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
                final String sql = String.format(SQL.SELECT.CONFIGS, mapping.get(CONFIGS_TABLE),
                        mapping.get(CONFIG_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE),
                        mapping.get(PROPERTY_ATTRIBUTES_TABLE));
                final String subSql = " OR `C`.`NAME` = ?";
                try (final Connection connection = dataSource.getConnection();
                     final PreparedStatement statement =
                             connection.prepareStatement(JDBCUtils.concatSql(sql, subSql, names))) {
                    JDBCUtils.set(statement, names);

                    try (final ResultSet resultSet = statement.executeQuery()) {
                        long prevConfigId = -1;
                        final Map<Long, Config> configs = new HashMap<>();
                        final Map<Long, Property> properties = new HashMap<>();
                        final Collection<SimpleEntry<Long, Long>> links = new LinkedList<>();
                        while (resultSet.next()) {
                            // Create properties
                            final long propertyId = resultSet.getLong(8);
                            if (propertyId > 0) {
                                final Property.Builder builder;
                                final Property property = properties.get(propertyId);
                                final Optional<SimpleEntry<String, String>> optional =
                                        getAttributes(resultSet.getString(16), resultSet.getString(17));
                                if (property == null) {
                                    builder = new Property.Builder(resultSet.getString(10),
                                            resultSet.getString(13),
                                            resultSet.getString(14)).
                                            id(propertyId).
                                            caption(resultSet.getString(11)).
                                            description(resultSet.getString(12)).
                                            updated(resultSet.getLong(15));
                                    optional.ifPresent(a -> builder.attribute(a.getKey(), a.getValue()));
                                } else {
                                    builder = new Property.Builder(property);
                                    optional.ifPresent(a -> builder.attribute(a.getKey(), a.getValue()));
                                }
                                // Set a property
                                properties.put(propertyId, builder.build());
                                // Create links
                                links.add(new SimpleEntry<>(propertyId, resultSet.getLong(9)));
                            }
                            // Create configs
                            final Config.Builder builder;
                            final long configId = resultSet.getInt(1);
                            final Config config = configs.get(configId);
                            final Optional<SimpleEntry<String, String>> optional =
                                    getAttributes(resultSet.getString(6), resultSet.getString(7));
                            if (config == null) {
                                builder = new Config.Builder(resultSet.getString(2), Collections.emptyList()).
                                        id(configId).
                                        description(resultSet.getString(3)).
                                        version(resultSet.getInt(4)).
                                        updated(resultSet.getLong(5));
                                optional.ifPresent(a -> builder.attribute(a.getKey(), a.getValue()));
                            } else {
                                builder = new Config.Builder(config);
                                optional.ifPresent(a -> builder.attribute(a.getKey(), a.getValue()));
                            }
                            // Set a config
                            configs.put(configId, builder.build());
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
            final String sql = String.format(SQL.SELECT.CONFIG_NAMES, mapping.get(CONFIGS_TABLE));
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
            throw new RuntimeException(RECEIVED_CONFIG_NAMES_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse findByPageRequest(final PageRequest request) {
        try {
            final String configs = mapping.get(CONFIGS_TABLE);
            final String attributes = mapping.get(CONFIG_ATTRIBUTES_TABLE);
            final String sql = String.format(SQL.SELECT.CONFIG_NAMES_BY_NAME, configs, attributes) +
                    JDBCUtils.getSubSql(request) + " ORDER BY `C`.`NAME` " + (request.isAscending() ? "ASC" : "DESC") +
                    " LIMIT " + request.getSize() + " OFFSET " + request.getPage() * request.getSize() + ";";
            try (final Connection connection = dataSource.getConnection()) {
                final int total = getCount(connection, configs, attributes, request);
                if (total > 0) {
                    try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                        JDBCUtils.set(statement, request);

                        try (final ResultSet resultSet = statement.executeQuery()) {
                            final Collection<String> names = new LinkedList<>();
                            while (resultSet.next()) {
                                names.add(resultSet.getString(1));
                            }

                            return new PageResponse.Builder(names).
                                    page(request.getPage()).
                                    total(total).
                                    build();
                        }
                    }
                }

                return new PageResponse.Builder(Collections.emptyList()).
                        page(request.getPage()).
                        total(total).
                        build();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(RECEIVED_PAGE_RESPONSE_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> saveAndFlush(final Stream<Config> stream) {
        Connection connection = null;
        Stream<Config> configs = Stream.empty();
        try {
            connection = JDBCUtils.open(dataSource);
            configs = Arrays.stream(saveAndFlush(connection, stream.toArray(Config[]::new)));
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }

        return configs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(final Stream<String> stream) {
        Connection connection = null;
        int count = 0;
        try {
            connection = JDBCUtils.open(dataSource);
            count = delete(connection, stream.toArray(String[]::new));
        } catch (final SQLException e) {
            JDBCUtils.rollback(connection, e);
        } finally {
            JDBCUtils.close(connection);
        }

        return count;
    }

    private Collection<Property> getLinkedProps(final Map<Long, Property> properties,
                                                final Collection<SimpleEntry<Long, Long>> links) {
        final Map<Long, Property> linkedProps = new HashMap<>(properties);
        final Comparator<SimpleEntry<Long, Long>> comparing = Comparator.comparing(SimpleEntry::getValue);
        links.stream().
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

    private Optional<SimpleEntry<String, String>> getAttributes(final String key, final String value) {
        return key != null && value != null ? Optional.of(new SimpleEntry<>(key, value)) : Optional.empty();
    }

    private Config[] saveAndFlush(final Connection connection, final Config[] configs) throws SQLException {
        final Collection<Config> toUpdate = new LinkedList<>();
        final Collection<Config> toInsert = new LinkedList<>();
        for (final Config config : configs) {
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

        if (savedConfigs.length > 0) {
            connection.commit();
        }

        return savedConfigs;
    }

    private Config[] insert(final Connection connection, final Config[] configs) throws SQLException {
        Config[] inserted = new Config[0];
        if (configs.length > 0) {
            inserted = new Config[configs.length];
            final String configsSql = String.format(SQL.INSERT.CONFIGS, mapping.get(CONFIGS_TABLE));
            try (final PreparedStatement statement =
                         connection.prepareStatement(configsSql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Config config : configs) {
                    JDBCUtils.set(statement, config, 1);
                    statement.addBatch();
                }

                if (statement.executeBatch().length == configs.length) {
                    try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                        final Collection<Throwable> exceptions = new LinkedList<>();
                        for (int i = 0; i < configs.length; i++) {
                            resultSet.absolute(i + 1);
                            final long configId = resultSet.getInt(1);
                            final Property[] properties = configs[i].getProperties().toArray(Property[]::new);
                            // Create config attributes
                            configs[i].getAttributes().ifPresent(a -> {
                                try {
                                    final String attributesSql = String.format(SQL.INSERT.CONFIG_ATTRIBUTES,
                                            mapping.get(CONFIG_ATTRIBUTES_TABLE));
                                    execute(connection, attributesSql, configId, a, INSERT_ATTRIBUTES_ERROR);
                                } catch (final SQLException e) {
                                    exceptions.add(e);
                                }
                            });
                            // Update a config
                            inserted[i] = new Config.Builder(configs[i]).
                                    id(configId).
                                    properties(Arrays.asList(insert(connection, configId, properties))).
                                    build();
                        }

                        if (exceptions.size() > 0) {
                            throw new SQLException(String.format(INSERT_ATTRIBUTES_ERROR,
                                    JDBCUtils.getDetails(exceptions)));
                        }
                    }
                } else {
                    throw new SQLException(SAVE_CONFIGS_ERROR);
                }
            }
        }

        return inserted;
    }

    private void execute(final Connection connection, final String sql, final long id,
                         final Map<String, String> attributes, final String error) throws SQLException {
        if (attributes.size() > 0) {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                JDBCUtils.setBatch(statement, id, attributes);

                if (statement.executeBatch().length != attributes.size()) {
                    throw new SQLException(error);
                }
            }
        }
    }

    private void execute(final Connection connection, final String sql, final long id,
                         final Map<String, String> attributes) throws SQLException {
        if (attributes.size() > 0) {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                JDBCUtils.setBatch(statement, attributes, id);

                if (statement.executeBatch().length != attributes.size()) {
                    throw new SQLException(UPDATE_ATTRIBUTES_ERROR);
                }
            }
        }
    }

    private Property[] insert(final Connection connection, final long id, final Property... properties)
            throws SQLException {
        if (properties.length > 0) {
            final String sql = String.format(SQL.INSERT.PROPERTIES, mapping.get(PROPERTIES_TABLE));
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Property property : properties) {
                    JDBCUtils.setBatch(statement, id, property);
                }

                return insert(connection, statement, id, properties);
            }
        }

        return properties;
    }

    private Property[] insert(final Connection connection, final SimpleEntry<Long, Long> confPropIds,
                              final Property... properties) throws SQLException {
        if (properties.length > 0) {
            final String sql = String.format(SQL.INSERT.SUB_PROPERTIES, mapping.get(PROPERTIES_TABLE));
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (final Property property : properties) {
                    JDBCUtils.setBatch(statement, confPropIds, property);
                }

                return insert(connection, statement, confPropIds.getKey(), properties);
            }
        }

        return properties;
    }

    private Property[] insert(final Connection connection, final PreparedStatement statement, final long configId,
                              final Property... properties) throws SQLException {
        if (statement.executeBatch().length == properties.length) {
            try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (int i = 0; i < properties.length; i++) {
                    resultSet.absolute(i + 1);
                    final long propertyId = resultSet.getLong(1);
                    // Create property attributes
                    properties[i].getAttributes().ifPresent(a -> {
                        try {
                            final String sql = String.format(SQL.INSERT.PROPERTY_ATTRIBUTES,
                                    mapping.get(PROPERTY_ATTRIBUTES_TABLE));
                            execute(connection, sql, propertyId, a, INSERT_ATTRIBUTES_ERROR);
                        } catch (final SQLException e) {
                            exceptions.add(e);
                        }
                    });
                    // Update a property
                    properties[i] = new Property.Builder(properties[i]).
                            properties(Arrays.asList(insert(connection, new SimpleEntry<>(configId, propertyId),
                                    properties[i].getProperties().toArray(Property[]::new)))).
                            id(propertyId).build();
                }

                if (exceptions.size() > 0) {
                    throw new SQLException(String.format(INSERT_ATTRIBUTES_ERROR, JDBCUtils.getDetails(exceptions)));
                }
            }
        } else {
            throw new SQLException(SAVE_PROPERTIES_ERROR);
        }

        return properties;
    }

    private Config[] update(final Connection connection, final Config[] configs) throws SQLException {
        Config[] updated = new Config[0];
        if (configs.length > 0) {
            updated = new Config[configs.length];
            final String sql = String.format(SQL.UPDATE.CONFIGS, mapping.get(CONFIGS_TABLE));
            final Map<Long, SimpleEntry<Integer, Long>> entries = getVerUpdEntries(connection, configs);
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (int i = 0; i < configs.length; i++) {
                    final Config config = configs[i];
                    final SimpleEntry<Integer, Long> entry = entries.get(config.getId());
                    if (config.getUpdated() > entry.getValue()) {
                        final int version = entry.getKey() + 1;
                        statement.setLong(5, config.getId());
                        statement.setInt(6, config.getVersion());
                        JDBCUtils.set(statement, config, version);
                        statement.addBatch();
                        // Update config attributes
                        config.getAttributes().ifPresent(a -> {
                            try {
                                update(connection, TableId.CONFIG, mapping.get(CONFIG_ATTRIBUTES_TABLE),
                                        config.getId(), a);
                            } catch (final SQLException e) {
                                exceptions.add(e);
                            }
                        });
                        // Update a config
                        updated[i] = new Config.Builder(configs[i]).
                                properties(Arrays.asList(update(connection, mapping.get(PROPERTIES_TABLE),
                                        config.getId(), config.getProperties().toArray(Property[]::new)))).
                                version(version).
                                build();
                    }
                }

                JDBCUtils.execute(statement, configs.length, exceptions, SAVE_CONFIGS_ERROR);
            }
        }

        return updated;
    }

    private void update(final Connection connection, final TableId tableId, final String table, final long id,
                        final Map<String, String> attributes) throws SQLException {
        if (attributes.size() > 0) {
            final Map<String, String> toInsert = new HashMap<>();
            final Map<String, String> toUpdate = new HashMap<>();
            final Map<String, String> toDelete = new HashMap<>();
            final Map<String, String> existed = new HashMap<>();
            if (TableId.CONFIG.equals(tableId)) {
                existed.putAll(getAttributes(connection, String.format(SQL.SELECT.CONFIG_ATTRIBUTES, table), id));
            } else if (TableId.PROPERTY.equals(tableId)) {
                existed.putAll(getAttributes(connection, String.format(SQL.SELECT.PROPERTY_ATTRIBUTES, table), id));
            }

            update(existed, attributes, toDelete, toUpdate);
            update(attributes, existed, toInsert, toUpdate);

            if (TableId.CONFIG.equals(tableId)) {
                execute(connection, String.format(SQL.INSERT.CONFIG_ATTRIBUTES, table), id, toInsert,
                        UPDATE_ATTRIBUTES_ERROR);
                execute(connection, String.format(SQL.UPDATE.ATTRIBUTE, table), id, toUpdate);
                execute(connection, String.format(SQL.DELETE.CONFIG_ATTRIBUTE, table), id, toDelete,
                        UPDATE_ATTRIBUTES_ERROR);
            } else if (TableId.PROPERTY.equals(tableId)) {
                execute(connection, String.format(SQL.INSERT.PROPERTY_ATTRIBUTES, table), id, toInsert,
                        UPDATE_ATTRIBUTES_ERROR);
                execute(connection, String.format(SQL.UPDATE.ATTRIBUTE, table), id, toUpdate);
                execute(connection, String.format(SQL.DELETE.PROPERTY_ATTRIBUTE, table), id, toDelete,
                        UPDATE_ATTRIBUTES_ERROR);
            }
        } else {
            if (TableId.CONFIG.equals(tableId)) {
                delete(connection, String.format(SQL.DELETE.CONFIG_ATTRIBUTES, table), id);
            } else if (TableId.PROPERTY.equals(tableId)) {
                delete(connection, String.format(SQL.DELETE.PROPERTY_ATTRIBUTES, table), id);
            }
        }
    }

    private void update(final Map<String, String> source, final Map<String, String> target,
                        final Map<String, String> replace, final Map<String, String> update) {
        for (final String key : source.keySet()) {
            final String value = source.get(key);
            if (!target.containsKey(key)) {
                replace.put(key, value);
            } else {
                if (!value.equals(target.get(key))) {
                    update.put(key, value);
                }
            }
        }
    }

    private Property[] update(final Connection connection, final String table, final long id,
                              final Property[] properties) throws SQLException {
        if (properties.length > 0) {
            final Map<Long, Long> idUpdated =
                    getIdUpdated(connection, String.format(SQL.SELECT.PROPERTY_ID_UPDATED, table), id);
            final Collection<Property> toUpdate = getToUpdate(connection, id, 0, idUpdated, properties);
            // Delete old properties
            for (final long propertyId : idUpdated.keySet()) {
                delete(connection, String.format(SQL.DELETE.PROPERTY, table), propertyId);
            }
            // Update properties
            if (toUpdate.size() > 0) {
                update(connection, table, toUpdate.toArray(new Property[0]));
            }
        } else {
            delete(connection, String.format(SQL.DELETE.PROPERTIES, table), id);
        }

        return properties;
    }

    private Collection<Property> getToUpdate(final Connection connection, final long configId, final long propertyId,
                                             final Map<Long, Long> idUpdated,
                                             final Property[] properties) throws SQLException {
        final Collection<Property> toUpdate = new LinkedList<>();
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].getId() > 0) {
                final Long updated = idUpdated.get(properties[i].getId());
                if (updated != null && properties[i].getUpdated() > updated) {
                    toUpdate.add(properties[i]);
                }
            } else {
                properties[i] = propertyId > 0 ?
                        insert(connection, new SimpleEntry<>(configId, propertyId), properties[i])[0] :
                        insert(connection, configId, properties[i])[0];
            }
            // Set indices to delete
            idUpdated.remove(properties[i].getId());
            // Update sub-properties
            toUpdate.addAll(getToUpdate(connection, configId, idUpdated, properties[i]));
        }

        return toUpdate;
    }

    private Collection<Property> getToUpdate(final Connection connection, final long id, final Map<Long, Long> idUpdated,
                                             final Property property) throws SQLException {
        final Collection<Property> toUpdate = new LinkedList<>();
        final Property[] properties = property.getProperties().toArray(Property[]::new);
        if (properties.length > 0) {
            toUpdate.addAll(getToUpdate(connection, id, property.getId(), idUpdated, properties));
        }

        return toUpdate;
    }

    private void update(final Connection connection, final String table, final Property[] properties)
            throws SQLException {
        if (properties.length > 0) {
            final String sql = String.format(SQL.UPDATE.PROPERTIES, table);
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (final Property property : properties) {
                    JDBCUtils.setBatch(statement, property);
                    // Update property attributes
                    property.getAttributes().ifPresent(a -> {
                        try {
                            update(connection, TableId.PROPERTY, mapping.get(PROPERTY_ATTRIBUTES_TABLE),
                                    property.getId(), a);
                        } catch (final SQLException e) {
                            exceptions.add(e);
                        }
                    });
                }

                JDBCUtils.execute(statement, properties.length, exceptions, SAVE_PROPERTIES_ERROR);
            }
        }
    }

    private Map<Long, Long> getIdUpdated(final Connection connection, final String sql, final long id)
            throws SQLException {
        final Map<Long, Long> attributes = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attributes.put(resultSet.getLong(1), resultSet.getLong(2));
                }
            }
        }

        return attributes;
    }

    private Map<String, String> getAttributes(final Connection connection, final String sql, final long id)
            throws SQLException {
        final Map<String, String> attributes = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attributes.put(resultSet.getString(1), resultSet.getString(2));
                }
            }
        }

        return attributes;
    }

    private int getCount(final Connection connection, final String configs, final String attributes,
                         final PageRequest request) throws SQLException {
        int count = 0;
        final String sql = String.format(SQL.SELECT.COUNT_CONFIG_NAMES_BY_NAME, configs, attributes) +
                JDBCUtils.getSubSql(request) + ";";
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            JDBCUtils.set(statement, request);

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    count = resultSet.getInt(1);
                }
            }
        }

        return count;
    }

    private Map<Long, SimpleEntry<Integer, Long>> getVerUpdEntries(final Connection connection, final Config[] configs)
            throws SQLException {
        final Map<Long, SimpleEntry<Integer, Long>> entries = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        sql.append(String.format(SQL.SELECT.CONFIG_VERSION_UPDATED, mapping.get(CONFIGS_TABLE)));
        for (int i = 0; i < configs.length; i++) {
            if (i > 0) {
                sql.append(" OR ");
            }

            sql.append("`C`.`ID` = ?");
            if (i == configs.length - 1) {
                sql.append(";");
            }

            entries.put(configs[i].getId(), new SimpleEntry<>(configs[i].getVersion(), configs[i].getUpdated()));
        }

        try (final PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < configs.length; i++) {
                statement.setLong(i + 1, configs[i].getId());
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.put(resultSet.getLong(1),
                            new SimpleEntry<>(resultSet.getInt(2), resultSet.getLong(3)));
                }
            }
        }

        return entries;
    }

    private int delete(final Connection connection, final String[] names) throws SQLException {
        if (names.length > 0) {
            try {
                final String configs = mapping.get(CONFIGS_TABLE);
                final String sql = String.format(SQL.DELETE.CONFIGS, configs);
                final String subSql = String.format(" OR `%s`.`NAME` = ?", configs);
                try (final PreparedStatement statement =
                             connection.prepareStatement(JDBCUtils.concatSql(sql, subSql, names))) {
                    JDBCUtils.set(statement, names);
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

    private void delete(final Connection connection, final String sql, final long id) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private enum TableId {
        CONFIG, PROPERTY
    }

    private final static class SQL {

        private final static class INSERT {

            static final String CONFIGS =
                    "INSERT INTO `%s` (`NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (?, ?, ?, ?);";
            static final String CONFIG_ATTRIBUTES =
                    "INSERT INTO `%s` (`CONFIG_ID`, `KEY`, `VALUE`) VALUES (?, ?, ?);";
            static final String PROPERTIES =
                    "INSERT INTO `%s` (`CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `UPDATED`) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?);";
            static final String PROPERTY_ATTRIBUTES =
                    "INSERT INTO `%s` (`PROPERTY_ID`, `KEY`, `VALUE`) VALUES (?, ?, ?);";
            static final String SUB_PROPERTIES =
                    "INSERT INTO `%s` (`PROPERTY_ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, " +
                            "`UPDATED`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        }

        private final static class UPDATE {

            static final String CONFIGS =
                    "UPDATE `%s` SET `NAME` = ?, `DESCRIPTION` = ?, `VERSION` = ?, `UPDATED` = ? " +
                            "WHERE `ID` = ? AND `VERSION` = ?;";
            static final String ATTRIBUTE =
                    "UPDATE `%s` SET `KEY` = ?, `VALUE` = ? WHERE `ID` = ?;";
            static final String PROPERTIES =
                    "UPDATE `%s` SET `NAME` = ?, `CAPTION` = ?, `DESCRIPTION` = ?, `TYPE` = ?, `VALUE` = ?, " +
                            "`UPDATED` = ? WHERE `ID` = ?;";
        }

        private final static class DELETE {

            static final String CONFIGS =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`NAME` = ?";
            static final String CONFIG_ATTRIBUTES =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`CONFIG_ID` = ?;";
            static final String PROPERTY_ATTRIBUTES =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`PROPERTY_ID` = ?;";
            static final String CONFIG_ATTRIBUTE =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`CONFIG_ID` = ? AND `%1$s`.`KEY` = ? AND `%1$s`.`VALUE` = ?;";
            static final String PROPERTY_ATTRIBUTE =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`PROPERTY_ID` = ? AND `%1$s`.`KEY` = ? AND `%1$s`.`VALUE` = ?;";
            static final String PROPERTIES =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`CONFIG_ID` = ?;";
            static final String PROPERTY =
                    "DELETE FROM `%1$s` WHERE `%1$s`.`ID` = ?;";
        }

        private final static class SELECT {

            static final String CONFIG_NAMES =
                    "SELECT `C`.`NAME` FROM `%s` AS `C` ORDER BY `C`.`NAME`;";
            static final String COUNT_CONFIG_NAMES_BY_NAME =
                    "SELECT COUNT(DISTINCT `C`.`NAME`) FROM `%1$s` AS `C` " +
                            "INNER JOIN `%2$s` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID` " +
                            "WHERE (`C`.`NAME` LIKE ?)";
            static final String CONFIG_NAMES_BY_NAME =
                    "SELECT DISTINCT `C`.`NAME` FROM `%1$s` AS `C` " +
                            "INNER JOIN `%2$s` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID` " +
                            "WHERE (`C`.`NAME` LIKE ?)";
            static final String CONFIG_ATTRIBUTES =
                    "SELECT `CA`.`KEY`, `CA`.`VALUE` FROM `%s` AS `CA` WHERE `CA`.`CONFIG_ID` = ?;";
            static final String PROPERTY_ATTRIBUTES =
                    "SELECT `PA`.`KEY`, `PA`.`VALUE` FROM `%s` AS `PA` WHERE `PA`.`ID` = ?;";
            static final String PROPERTY_ID_UPDATED =
                    "SELECT `P`.`ID`, `P`.`UPDATED` FROM `%s` AS `P` WHERE `P`.`CONFIG_ID` = ?;";
            static final String CONFIG_VERSION_UPDATED =
                    "SELECT `C`.`ID`, `C`.`VERSION`, `C`.`UPDATED` FROM `%s` AS `C` WHERE ";
            static final String CONFIGS =
                    "SELECT `C`.`ID`, `C`.`NAME`, `C`.`DESCRIPTION`, `C`.`VERSION`, `C`.`UPDATED`, `CA`.`KEY`, " +
                            "`CA`.`VALUE`, `P`.`ID`, `P`.`PROPERTY_ID`, `P`.`NAME` , `P`.`CAPTION`, " +
                            "`P`.`DESCRIPTION`, `P`.`TYPE`, `P`.`VALUE`, `P`.`UPDATED`, `PA`.`KEY`, `PA`.`VALUE` " +
                            "FROM `%1$s` AS `C` " +
                            "LEFT JOIN `%3$s` AS `P` ON `C`.`ID` = `P`.`CONFIG_ID` " +
                            "LEFT JOIN `%2$s` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID` " +
                            "LEFT JOIN `%4$s` AS `PA` ON `P`.`ID` = `PA`.`PROPERTY_ID` " +
                            "WHERE `C`.`NAME` = ?";
        }

        private final static class CREATE_TABLE {

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
                            "`UPDATED` BIGINT NOT NULL," +
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
        private static final int FETCH_SIZE = 100;

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

        private static void createTables(final Connection connection, final Map<String, String> mapping)
                throws SQLException {
            try {
                try (final Statement statement = connection.createStatement()) {
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.CONFIGS, mapping.get(CONFIGS_TABLE)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.CONFIG_ATTRIBUTES,
                            mapping.get(CONFIGS_TABLE), mapping.get(CONFIG_ATTRIBUTES_TABLE)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.PROPERTIES,
                            mapping.get(PROPERTIES_TABLE), mapping.get(CONFIGS_TABLE)));
                    statement.executeUpdate(String.format(SQL.CREATE_TABLE.PROPERTY_ATTRIBUTES,
                            mapping.get(PROPERTY_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE)));
                    connection.commit();
                }
            } catch (final SQLException e) {
                throw new SQLException(CREATE_CONFIG_TABLE_ERROR, e);
            }
        }

        private static Map<String, String> createMapping(final Map<String, String> mapping) {
            if (mapping == null) {
                final Map<String, String> defaultMapping = new HashMap<>();
                defaultMapping.put(CONFIGS_TABLE, "CONFIGS");
                defaultMapping.put(CONFIG_ATTRIBUTES_TABLE, "CONFIG_ATTRIBUTES");
                defaultMapping.put(PROPERTIES_TABLE, "PROPERTIES");
                defaultMapping.put(PROPERTY_ATTRIBUTES_TABLE, "PROPERTY_ATTRIBUTES");
                return defaultMapping;
            } else {
                mapping.putIfAbsent(CONFIGS_TABLE, "CONFIGS");
                mapping.putIfAbsent(CONFIG_ATTRIBUTES_TABLE, "CONFIG_ATTRIBUTES");
                mapping.putIfAbsent(PROPERTIES_TABLE, "PROPERTIES");
                mapping.putIfAbsent(PROPERTY_ATTRIBUTES_TABLE, "PROPERTY_ATTRIBUTES");
            }

            return mapping;
        }

        private static String concatSql(final String sql, final String subSql, final String[] names) {
            final StringBuilder string = new StringBuilder(sql);
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> string.append(subSql));
            }

            string.append(";");
            return string.toString();
        }

        private static String getSubSql(final PageRequest request) {
            final StringBuilder string = new StringBuilder();
            final int size = request.getAttributes().size();
            for (int i = 0; i < size; i++) {
                string.append(i == 0 ? " AND" : " OR");
                string.append(" (`CA`.`KEY` LIKE ? AND `CA`.`VALUE` LIKE ?)");
            }

            return string.toString();
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

        private static void set(final PreparedStatement statement, final Config config, final int version)
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

        private static void set(final PreparedStatement statement, final String[] names) throws SQLException {
            statement.setFetchSize(FETCH_SIZE);
            for (int i = 0; i < names.length; i++) {
                statement.setString(i + 1, names[i]);
            }
        }

        private static void set(final PreparedStatement statement, final PageRequest request) throws SQLException {
            statement.setFetchSize(FETCH_SIZE);
            statement.setString(1, "%" + request.getName() + "%");

            int index = 1;
            final Map<String, String> attributes = request.getAttributes();
            for (final String key : attributes.keySet()) {
                statement.setString(++index, "%" + key + "%");
                statement.setString(++index, "%" + attributes.get(key) + "%");
            }
        }

        private static void setBatch(final PreparedStatement statement, final Map<String, String> attributes,
                                     final long id) throws SQLException {
            for (final String key : attributes.keySet()) {
                statement.setString(1, key);
                statement.setString(2, attributes.get(key));
                statement.setLong(3, id);
                statement.addBatch();
            }
        }

        private static void setBatch(final PreparedStatement statement, final long id,
                                     final Map<String, String> attributes) throws SQLException {
            for (final String key : attributes.keySet()) {
                statement.setLong(1, id);
                statement.setString(2, key);
                statement.setString(3, attributes.get(key));
                statement.addBatch();
            }
        }

        private static void setBatch(final PreparedStatement statement, final long id, final Property property)
                throws SQLException {
            statement.setLong(1, id);
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
            statement.setLong(7, property.getUpdated());
            statement.addBatch();
        }

        private static void setBatch(final PreparedStatement statement, final Property property) throws SQLException {
            statement.setString(1, property.getName());
            if (property.getCaption().isPresent()) {
                statement.setString(2, property.getCaption().get());
            } else {
                statement.setString(2, null);
            }

            if (property.getDescription().isPresent()) {
                statement.setString(3, property.getDescription().get());
            } else {
                statement.setString(3, null);
            }

            statement.setString(4, property.getType().name());
            statement.setString(5, property.getValue());
            statement.setLong(6, property.getUpdated());
            statement.setLong(7, property.getId());

            statement.addBatch();
        }

        private static void setBatch(final PreparedStatement statement, final SimpleEntry<Long, Long> confPropIds,
                                     final Property property) throws SQLException {
            statement.setLong(1, confPropIds.getValue());
            statement.setLong(2, confPropIds.getKey());
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
            statement.setLong(8, property.getUpdated());
            statement.addBatch();
        }

        private static void execute(final PreparedStatement statement, int count, final Collection<Throwable> exceptions,
                                    final String error) throws SQLException {
            if (statement.executeBatch().length == count) {
                if (exceptions.size() > 0) {
                    throw new SQLException(UPDATE_ATTRIBUTES_ERROR);
                }

                if (statement.getUpdateCount() == 0) {
                    throw new SQLException(error);
                }
            } else {
                throw new SQLException(error);
            }
        }

        private static String getDetails(final Collection<Throwable> exceptions) {
            final StringBuilder details = new StringBuilder();
            exceptions.forEach(e -> {
                if (details.length() > 0) {
                    details.append(", ");
                }

                details.append(e.getMessage());
            });
            return details.toString();
        }
    }

    /**
     * Wraps and builds the instance of the DB config repository.
     */
    final static class Builder {
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
        Builder mapping(final Map<String, String> mapping) {
            this.mapping = Validator.of(mapping).
                    validate(m -> validate(m, CONFIGS_TABLE), CONFIGS_TABLE + " mapping is wrong.").
                    validate(m -> validate(m, CONFIG_ATTRIBUTES_TABLE), CONFIG_ATTRIBUTES_TABLE +
                            " mapping is wrong.").
                    validate(m -> validate(m, PROPERTIES_TABLE), PROPERTIES_TABLE + " mapping is wrong.").
                    validate(m -> validate(m, PROPERTY_ATTRIBUTES_TABLE), PROPERTY_ATTRIBUTES_TABLE +
                            " mapping is wrong.").get();
            return this;
        }

        /**
         * Builds a DB config repository with a required parameter.
         *
         * @return a builder of the DB config repository.
         */
        ConfigRepository build() {
            return new DbConfigRepository(this);
        }

        private boolean validate(final Map<String, String> mapping, final String key) {
            if (mapping.containsKey(key)) {
                final String configs = mapping.get(key);
                return configs != null && configs.length() > 0;
            }

            return true;
        }
    }
}
