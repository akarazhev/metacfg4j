/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.api.sql.PostgreSQL;
import com.github.akarazhev.metaconfig.api.sql.SQL;
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
import java.util.LinkedHashSet;
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
import static com.github.akarazhev.metaconfig.Constants.Messages.INSERT_ATTRIBUTES_ERROR_MSG;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIG_NAMES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_PAGE_RESPONSE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_PROPERTIES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_ATTRIBUTES_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.UPDATE_ATTRIBUTES_ERROR_MSG;
import static com.github.akarazhev.metaconfig.Constants.Settings.DB_DIALECT;
import static com.github.akarazhev.metaconfig.Constants.Settings.DEFAULT;
import static com.github.akarazhev.metaconfig.Constants.Settings.FETCH_SIZE;
import static com.github.akarazhev.metaconfig.Constants.Settings.POSTGRE;
import static java.util.AbstractMap.SimpleEntry;

/**
 * {@inheritDoc}
 */
final class DbConfigRepository implements ConfigRepository {
    private final DataSource dataSource;
    private final SQLUtils sqlUtils;

    private DbConfigRepository(final Builder builder) {
        this.dataSource = builder.dataSource;
        this.sqlUtils = new SQLUtils(JDBCUtils.createMapping(builder.mapping), JDBCUtils.createSettings(builder.settings));
        JDBCUtils.createDataBase(this.dataSource, sqlUtils);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(final Stream<String> stream) {
        final String[] names = stream.toArray(String[]::new);
        if (names.length > 0) {
            try {
                final String sql = sqlUtils.select.configs();
                try (final Connection connection = dataSource.getConnection();
                     final PreparedStatement statement =
                             connection.prepareStatement(JDBCUtils.concatSql(sql, " OR C.NAME = ?", names))) {
                    JDBCUtils.set(statement, (Integer) sqlUtils.settings.get(FETCH_SIZE), names);

                    try (final ResultSet resultSet = statement.executeQuery()) {
                        long prevConfigId = -1;
                        final Map<Long, Config> configs = new HashMap<>();
                        final Map<Long, Property> properties = new HashMap<>();
                        final Collection<SimpleEntry<Long, Long>> links = new LinkedHashSet<>();
                        while (resultSet.next()) {
                            final long configId = resultSet.getInt(1);
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
                                final long id = resultSet.getLong(9);
                                if (id > 0) {
                                    links.add(new SimpleEntry<>(propertyId, id));
                                } else {
                                    links.add(new SimpleEntry<>(propertyId, configId));
                                }
                            }
                            // Create configs
                            final Config.Builder builder;
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
                                        properties(new String[0], getLinkedProps(prevConfigId, properties, links)).build());
                                links.clear();
                                properties.clear();
                            }

                            prevConfigId = configId;
                        }

                        if (configs.size() > 0) {
                            configs.put(prevConfigId, new Config.Builder(configs.get(prevConfigId)).
                                    properties(new String[0], getLinkedProps(prevConfigId, properties, links)).build());
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
            final String sql = sqlUtils.select.configNames();
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
            final String configs = sqlUtils.mapping.get(CONFIGS_TABLE);
            final String attributes = sqlUtils.mapping.get(CONFIG_ATTRIBUTES_TABLE);
            final String sql = sqlUtils.select.configNamesByName(request);
            try (final Connection connection = dataSource.getConnection()) {
                final int total = getCount(connection, configs, attributes, request);
                if (total > 0) {
                    try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                        JDBCUtils.set(statement, (Integer) sqlUtils.settings.get(FETCH_SIZE), request);

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

    private Collection<Property> getLinkedProps(final long configId, final Map<Long, Property> properties,
                                                final Collection<SimpleEntry<Long, Long>> links) {
        final Map<Long, Property> linkedProps = new HashMap<>(properties);
        final Comparator<Map.Entry<Long, Long>> comparing = Map.Entry.comparingByValue();
        links.stream().
                sorted(comparing.reversed()).
                forEach(link -> {
                    final Property childProp = linkedProps.get(link.getKey());
                    final Property parentProp = linkedProps.get(link.getValue());
                    if (childProp != null && parentProp != null) {
                        if (!childProp.equals(parentProp)) {
                            linkedProps.put(link.getValue(),
                                    new Property.Builder(parentProp).property(new String[0], childProp).build());
                            linkedProps.remove(link.getKey());
                        }
                    } else {
                        final Property prop = parentProp == null ? childProp : parentProp;
                        if (prop != null && !links.contains(new SimpleEntry<>(prop.getId(), configId))) {
                            linkedProps.remove(prop.getId());
                        }
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
            final String configsSql = sqlUtils.insert.configs();
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
                            resultSet.next();
                            final long configId = resultSet.getInt(1);
                            final Property[] properties = configs[i].getProperties().toArray(Property[]::new);
                            // Create config attributes
                            configs[i].getAttributes().ifPresent(a -> {
                                try {
                                    final String attributesSql =
                                            sqlUtils.insert.configAttributes(sqlUtils.mapping.get(CONFIG_ATTRIBUTES_TABLE));
                                    execute(connection, attributesSql, configId, a, INSERT_ATTRIBUTES_ERROR_MSG);
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
                            throw new SQLException(String.format(INSERT_ATTRIBUTES_ERROR_MSG,
                                    JDBCUtils.getMessage(exceptions)));
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

    private Property[] insert(final Connection connection, final long configId, final long propertyId,
                              final Property[] properties) throws SQLException {
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].getId() == 0) {
                properties[i] = propertyId > 0 ?
                        insert(connection, new SimpleEntry<>(configId, propertyId), properties[i])[0] :
                        insert(connection, configId, properties[i])[0];
            }
            // Insert sub-properties
            final Property[] subProps = insert(connection, configId, properties[i].getId(),
                    properties[i].properties().toArray(new Property[0]));
            properties[i] = new Property.Builder(properties[i]).properties(Arrays.asList(subProps)).build();
        }

        return properties;
    }

    private Property[] insert(final Connection connection, final long id, final Property... properties)
            throws SQLException {
        if (properties.length > 0) {
            final String sql = sqlUtils.insert.properties();
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
            final String sql = sqlUtils.insert.subProperties();
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
                    resultSet.next();
                    final long propertyId = resultSet.getLong(1);
                    // Create property attributes
                    properties[i].getAttributes().ifPresent(a -> {
                        try {
                            final String sql = sqlUtils.insert.propertyAttributes(sqlUtils.mapping.get(PROPERTY_ATTRIBUTES_TABLE));
                            execute(connection, sql, propertyId, a, INSERT_ATTRIBUTES_ERROR_MSG);
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
                    throw new SQLException(String.format(INSERT_ATTRIBUTES_ERROR_MSG, JDBCUtils.getMessage(exceptions)));
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
            final String sql = sqlUtils.update.configs();
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
                                update(connection, TableId.CONFIG, sqlUtils.mapping.get(CONFIG_ATTRIBUTES_TABLE),
                                        config.getId(), a);
                            } catch (final SQLException e) {
                                exceptions.add(e);
                            }
                        });
                        // Update a config
                        updated[i] = new Config.Builder(configs[i]).
                                properties(Arrays.asList(update(connection, sqlUtils.mapping.get(PROPERTIES_TABLE),
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
                existed.putAll(getAttributes(connection, sqlUtils.select.configAttributes(table), id));
            } else if (TableId.PROPERTY.equals(tableId)) {
                existed.putAll(getAttributes(connection, sqlUtils.select.propertyAttributes(table), id));
            }

            update(existed, attributes, toDelete, toUpdate);
            update(attributes, existed, toInsert, toUpdate);

            if (TableId.CONFIG.equals(tableId)) {
                execute(connection, sqlUtils.insert.configAttributes(table), id, toInsert,
                        UPDATE_ATTRIBUTES_ERROR_MSG);
                execute(connection, sqlUtils.update.configAttributes(table), id, toUpdate);
                execute(connection, sqlUtils.delete.configAttribute(table), id, toDelete,
                        UPDATE_ATTRIBUTES_ERROR_MSG);
            } else if (TableId.PROPERTY.equals(tableId)) {
                execute(connection, sqlUtils.insert.propertyAttributes(table), id, toInsert,
                        UPDATE_ATTRIBUTES_ERROR_MSG);
                execute(connection, sqlUtils.update.propertyAttributes(table), id, toUpdate);
                execute(connection, sqlUtils.delete.propertyAttribute(table), id, toDelete,
                        UPDATE_ATTRIBUTES_ERROR_MSG);
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
                    getIdUpdated(connection, sqlUtils.select.propertyIdUpdated(table), id);
            final Collection<Property> toUpdate = getToUpdate(connection, id, 0, idUpdated, properties);
            // Delete old properties
            for (final long propertyId : idUpdated.keySet()) {
                delete(connection, sqlUtils.delete.property(table), propertyId);
            }
            // Update properties
            if (toUpdate.size() > 0) {
                update(connection, table, toUpdate.toArray(new Property[0]));
            }
            // Insert properties
            return insert(connection, id, 0, properties);
        } else {
            delete(connection, sqlUtils.delete.properties(table), id);
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
            final String sql = sqlUtils.update.properties(table);
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                final Collection<Throwable> exceptions = new LinkedList<>();
                for (final Property property : properties) {
                    JDBCUtils.setBatch(statement, property);
                    // Update property attributes
                    property.getAttributes().ifPresent(a -> {
                        try {
                            update(connection, TableId.PROPERTY, sqlUtils.mapping.get(PROPERTY_ATTRIBUTES_TABLE),
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
        final String sql = sqlUtils.select.countConfigNamesByName(configs, attributes) +
                SQLUtils.getSubSql(request, sqlUtils.select.dialect) + ";";
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            JDBCUtils.set(statement, (Integer) sqlUtils.settings.get(FETCH_SIZE), request);

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
        sql.append(sqlUtils.select.configVersionUpdated());
        for (int i = 0; i < configs.length; i++) {
            if (i > 0) {
                sql.append(" OR ");
            }

            sql.append("C.ID = ?");
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
                final String configs = sqlUtils.mapping.get(CONFIGS_TABLE);
                final String sql = sqlUtils.delete.configs(configs);
                final String subSql = String.format(" OR %s.NAME = ?", configs);
                try (final PreparedStatement statement =
                             connection.prepareStatement(JDBCUtils.concatSql(sql, subSql, names))) {
                    JDBCUtils.set(statement, (Integer) sqlUtils.settings.get(FETCH_SIZE), names);
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

    private final static class SQLUtils {
        private final Map<String, String> mapping;
        private final Map<String, Object> settings;
        private final Create create;
        private final Select select;
        private final Insert insert;
        private final Update update;
        private final Delete delete;

        public SQLUtils(final Map<String, String> mapping, final Map<String, Object> settings) {
            this.mapping = mapping;
            this.settings = settings;

            this.create = new Create(mapping, settings);
            this.select = new Select(mapping, settings);
            this.insert = new Insert(mapping, settings);
            this.update = new Update(mapping, settings);
            this.delete = new Delete(settings);
        }

        private static String getSubSql(final PageRequest request, final String dialect) {
            final StringBuilder string = new StringBuilder();
            final int size = request.getAttributes().size();
            for (int i = 0; i < size; i++) {
                string.append(i == 0 ? " AND" : " OR");
                if (POSTGRE.equals(dialect)) {
                    string.append(" (CA.KEY LIKE ? AND CA.VALUE LIKE ?)");
                } else {
                    string.append(" (CA.`KEY` LIKE ? AND CA.`VALUE` LIKE ?)");
                }
            }

            return string.toString();
        }

        private static final class Create {
            private final String dialect;
            private final Map<String, String> mapping;

            public Create(final Map<String, String> mapping, final Map<String, Object> settings) {
                this.dialect = (String) settings.get(DB_DIALECT);
                this.mapping = mapping;
            }

            private String configs() {
                String sql;
                final String configsTable = mapping.get(CONFIGS_TABLE);
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.CREATE_TABLE.CONFIGS, configsTable);
                } else {
                    sql = String.format(SQL.CREATE_TABLE.CONFIGS, configsTable);
                }

                return sql;
            }

            private String configAttributes() {
                String sql;
                final String configsTable = mapping.get(CONFIGS_TABLE);
                final String configAttributeTable = mapping.get(CONFIG_ATTRIBUTES_TABLE);
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.CREATE_TABLE.CONFIG_ATTRIBUTES, configsTable, configAttributeTable);
                } else {
                    sql = String.format(SQL.CREATE_TABLE.CONFIG_ATTRIBUTES, configsTable, configAttributeTable);
                }

                return sql;
            }

            private String properties() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.CREATE_TABLE.PROPERTIES,
                            mapping.get(PROPERTIES_TABLE), mapping.get(CONFIGS_TABLE));
                } else {
                    sql = String.format(SQL.CREATE_TABLE.PROPERTIES,
                            mapping.get(PROPERTIES_TABLE), mapping.get(CONFIGS_TABLE));
                }

                return sql;
            }

            private String propertiesAttributes() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.CREATE_TABLE.PROPERTY_ATTRIBUTES,
                            mapping.get(PROPERTY_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE));
                } else {
                    sql = String.format(SQL.CREATE_TABLE.PROPERTY_ATTRIBUTES,
                            mapping.get(PROPERTY_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE));
                }

                return sql;
            }
        }

        private static final class Select {
            private final String dialect;
            private final Map<String, String> mapping;

            public Select(final Map<String, String> mapping, final Map<String, Object> settings) {
                this.dialect = (String) settings.get(DB_DIALECT);
                this.mapping = mapping;
            }

            private String configs() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.CONFIGS, mapping.get(CONFIGS_TABLE),
                            mapping.get(CONFIG_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE),
                            mapping.get(PROPERTY_ATTRIBUTES_TABLE));
                } else {
                    sql = String.format(SQL.SELECT.CONFIGS, mapping.get(CONFIGS_TABLE),
                            mapping.get(CONFIG_ATTRIBUTES_TABLE), mapping.get(PROPERTIES_TABLE),
                            mapping.get(PROPERTY_ATTRIBUTES_TABLE));
                }

                return sql;
            }

            private String configNames() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.CONFIG_NAMES, mapping.get(CONFIGS_TABLE));
                } else {
                    sql = String.format(SQL.SELECT.CONFIG_NAMES, mapping.get(CONFIGS_TABLE));
                }

                return sql;
            }

            private String configNamesByName(final PageRequest request) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.CONFIG_NAMES_BY_NAME, mapping.get(CONFIGS_TABLE),
                            mapping.get(CONFIG_ATTRIBUTES_TABLE)) +
                            getSubSql(request, dialect) + " ORDER BY C.NAME " + (request.isAscending() ? "ASC" : "DESC") +
                            " LIMIT " + request.getSize() + " OFFSET " + request.getPage() * request.getSize() + ";";
                } else {
                    sql = String.format(SQL.SELECT.CONFIG_NAMES_BY_NAME, mapping.get(CONFIGS_TABLE),
                            mapping.get(CONFIG_ATTRIBUTES_TABLE)) +
                            getSubSql(request, dialect) + " ORDER BY C.NAME " + (request.isAscending() ? "ASC" : "DESC") +
                            " LIMIT " + request.getSize() + " OFFSET " + request.getPage() * request.getSize() + ";";
                }

                return sql;
            }

            private String configAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.CONFIG_ATTRIBUTES, table);
                } else {
                    sql = String.format(SQL.SELECT.CONFIG_ATTRIBUTES, table);
                }

                return sql;
            }

            private String propertyAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.PROPERTY_ATTRIBUTES, table);
                } else {
                    sql = String.format(SQL.SELECT.PROPERTY_ATTRIBUTES, table);
                }

                return sql;
            }

            private String propertyIdUpdated(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.PROPERTY_ID_UPDATED, table);
                } else {
                    sql = String.format(SQL.SELECT.PROPERTY_ID_UPDATED, table);
                }

                return sql;
            }

            private String countConfigNamesByName(final String configs, final String attributes) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.COUNT_CONFIG_NAMES_BY_NAME, configs, attributes);
                } else {
                    sql = String.format(SQL.SELECT.COUNT_CONFIG_NAMES_BY_NAME, configs, attributes);
                }

                return sql;
            }

            private String configVersionUpdated() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.SELECT.CONFIG_VERSION_UPDATED, mapping.get(CONFIGS_TABLE));
                } else {
                    sql = String.format(SQL.SELECT.CONFIG_VERSION_UPDATED, mapping.get(CONFIGS_TABLE));
                }

                return sql;
            }
        }

        private static final class Insert {
            private final String dialect;
            private final Map<String, String> mapping;

            public Insert(final Map<String, String> mapping, final Map<String, Object> settings) {
                this.dialect = (String) settings.get(DB_DIALECT);
                this.mapping = mapping;
            }

            private String configs() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(String.format(PostgreSQL.INSERT.CONFIGS, mapping.get(CONFIGS_TABLE)));
                } else {
                    sql = String.format(String.format(SQL.INSERT.CONFIGS, mapping.get(CONFIGS_TABLE)));
                }

                return sql;
            }

            private String properties() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.INSERT.PROPERTIES, mapping.get(PROPERTIES_TABLE));
                } else {
                    sql = String.format(SQL.INSERT.PROPERTIES, mapping.get(PROPERTIES_TABLE));
                }

                return sql;
            }

            private String subProperties() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.INSERT.SUB_PROPERTIES, mapping.get(PROPERTIES_TABLE));
                } else {
                    sql = String.format(SQL.INSERT.SUB_PROPERTIES, mapping.get(PROPERTIES_TABLE));
                }

                return sql;
            }

            private String configAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.INSERT.CONFIG_ATTRIBUTES, table);
                } else {
                    sql = String.format(SQL.INSERT.CONFIG_ATTRIBUTES, table);
                }

                return sql;
            }

            private String propertyAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.INSERT.PROPERTY_ATTRIBUTES, table);
                } else {
                    sql = String.format(SQL.INSERT.PROPERTY_ATTRIBUTES, table);
                }

                return sql;
            }
        }

        private static final class Update {
            private final String dialect;
            private final Map<String, String> mapping;

            public Update(final Map<String, String> mapping, final Map<String, Object> settings) {
                this.dialect = (String) settings.get(DB_DIALECT);
                this.mapping = mapping;
            }

            private String configs() {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.UPDATE.CONFIGS, mapping.get(CONFIGS_TABLE));
                } else {
                    sql = String.format(SQL.UPDATE.CONFIGS, mapping.get(CONFIGS_TABLE));
                }

                return sql;
            }

            private String properties(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.UPDATE.PROPERTIES, table);
                } else {
                    sql = String.format(SQL.UPDATE.PROPERTIES, table);
                }

                return sql;
            }

            private String configAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.UPDATE.CONFIG_ATTRIBUTE, table);
                } else {
                    sql = String.format(SQL.UPDATE.CONFIG_ATTRIBUTE, table);
                }

                return sql;
            }

            private String propertyAttributes(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.UPDATE.PROPERTY_ATTRIBUTE, table);
                } else {
                    sql = String.format(SQL.UPDATE.PROPERTY_ATTRIBUTE, table);
                }

                return sql;
            }
        }

        private static final class Delete {
            private final String dialect;

            public Delete(final Map<String, Object> settings) {
                this.dialect = (String) settings.get(DB_DIALECT);
            }

            private String configs(final String configs) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.DELETE.CONFIGS, configs);
                } else {
                    sql = String.format(SQL.DELETE.CONFIGS, configs);
                }

                return sql;
            }

            private String configAttribute(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.DELETE.CONFIG_ATTRIBUTE, table);
                } else {
                    sql = String.format(SQL.DELETE.CONFIG_ATTRIBUTE, table);
                }

                return sql;
            }

            private String propertyAttribute(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.DELETE.PROPERTY_ATTRIBUTE, table);
                } else {
                    sql = String.format(SQL.DELETE.PROPERTY_ATTRIBUTE, table);
                }

                return sql;
            }

            private String property(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.DELETE.PROPERTY, table);
                } else {
                    sql = String.format(SQL.DELETE.PROPERTY, table);
                }

                return sql;
            }

            private String properties(final String table) {
                String sql;
                if (POSTGRE.equals(dialect)) {
                    sql = String.format(PostgreSQL.DELETE.PROPERTIES, table);
                } else {
                    sql = String.format(SQL.DELETE.PROPERTIES, table);
                }

                return sql;
            }
        }
    }

    private static final class JDBCUtils {

        private static void createDataBase(final DataSource dataSource, final SQLUtils sqlUtils) {
            Connection connection = null;
            try {
                connection = JDBCUtils.open(dataSource);
                createTables(connection, sqlUtils);
            } catch (final SQLException e) {
                JDBCUtils.rollback(connection, e);
            } finally {
                JDBCUtils.close(connection);
            }
        }

        private static void createTables(final Connection connection, final SQLUtils sqlUtils)
                throws SQLException {
            try {
                try (final Statement statement = connection.createStatement()) {
                    statement.executeUpdate(sqlUtils.create.configs());
                    statement.executeUpdate(sqlUtils.create.configAttributes());
                    statement.executeUpdate(sqlUtils.create.properties());
                    statement.executeUpdate(sqlUtils.create.propertiesAttributes());
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

        private static Map<String, Object> createSettings(final Map<String, Object> settings) {
            if (settings == null) {
                final Map<String, Object> defaultSettings = new HashMap<>();
                defaultSettings.put(FETCH_SIZE, 100);
                defaultSettings.put(DB_DIALECT, DEFAULT);
                return defaultSettings;
            } else {
                settings.putIfAbsent(FETCH_SIZE, 100);
                settings.putIfAbsent(DB_DIALECT, DEFAULT);
            }

            return settings;
        }

        private static String concatSql(final String sql, final String subSql, final String[] names) {
            final StringBuilder string = new StringBuilder(sql);
            if (names.length > 1) {
                Arrays.stream(names).skip(1).forEach(name -> string.append(subSql));
            }

            string.append(";");
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

        private static void set(final PreparedStatement statement, final int fetchSize, final String[] names)
                throws SQLException {
            statement.setFetchSize(fetchSize);
            for (int i = 0; i < names.length; i++) {
                statement.setString(i + 1, names[i]);
            }
        }

        private static void set(final PreparedStatement statement, final int fetchSize, final PageRequest request)
                throws SQLException {
            statement.setFetchSize(fetchSize);
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
                statement.setString(1, attributes.get(key));
                statement.setLong(2, id);
                statement.setString(3, key);
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
                    throw new SQLException(String.format(UPDATE_ATTRIBUTES_ERROR_MSG, JDBCUtils.getMessage(exceptions)));
                }

                if (statement.getUpdateCount() == 0) {
                    throw new SQLException(error);
                }
            } else {
                throw new SQLException(error);
            }
        }

        private static String getMessage(final Collection<Throwable> exceptions) {
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
        private Map<String, Object> settings;

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
         * Constructs a DB config repository with settings.
         *
         * @param settings DB settings.
         * @return a builder of the DB config repository.
         */
        Builder settings(final Map<String, Object> settings) {
            this.settings = Validator.of(settings).
                    validate(m -> {
                        if (settings.containsKey(FETCH_SIZE)) {
                            final Object value = settings.get(FETCH_SIZE);
                            return value instanceof Integer;
                        }

                        if (settings.containsKey(DB_DIALECT)) {
                            final Object value = settings.get(DB_DIALECT);
                            return POSTGRE.equals(value) || DEFAULT.equals(value);
                        }

                        return true;
                    }, FETCH_SIZE + " setting is wrong.").get();
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
