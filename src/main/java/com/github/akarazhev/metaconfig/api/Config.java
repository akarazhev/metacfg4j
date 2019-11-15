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

import com.github.cliftonlabs.json_simple.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_ID_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_UPDATED_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_VERSION_VALUE;

/**
 * The configuration model that contains parameters, attributes and properties.
 */
public final class Config extends AbstractConfig {
    private final int id;
    private final String name;
    private final String description;
    private final int version;
    private final long updated;
    private final Map<String, String> attributes;
    private final Collection<Property> properties;

    private Config(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.updated = builder.updated;
        this.attributes = builder.attributes;
        this.properties = builder.properties;
    }

    /**
     * Returns an id of the configuration.
     *
     * @return a configuration id.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a name of the configuration.
     *
     * @return a configuration name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a description of the configuration (optional).
     *
     * @return a configuration description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a version of the configuration.
     *
     * @return a version value.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns an updating time of the configuration.
     *
     * @return a configuration updated time value.
     */
    public long getUpdated() {
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Map<String, String>> getAttributes() {
        return Optional.of(Collections.unmodifiableMap(attributes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getAttributeKeys() {
        return attributes.keySet().stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getAttribute(final String key) {
        return Optional.ofNullable(attributes.get(Objects.requireNonNull(key)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Property> getProperties() {
        return properties.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Property> getProperty(final String... paths) {
        return getProperty(0, paths, getProperties());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(final Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("version", version);
        json.put("updated", updated);
        json.put("attributes", attributes);
        json.put("properties", properties);
        json.toJson(writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return id == config.id &&
                version == config.version &&
                updated == config.updated &&
                name.equals(config.name) &&
                Objects.equals(description, config.description) &&
                attributes.equals(config.attributes) &&
                properties.equals(config.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, version, updated, attributes, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", updated=" + updated +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration model.
     */
    public final static class Builder extends ConfigBuilder {
        private int id;
        private final String name;
        private String description;
        private int version;
        private long updated;

        /**
         * Constructs a configuration model based on the config object.
         *
         * @param config a configuration model.
         */
        public Builder(final Config config) {
            final Config prototype = Objects.requireNonNull(config);
            this.id = config.id;
            this.name = prototype.name;
            this.description = prototype.description;
            this.version = prototype.version;
            this.updated = prototype.updated;
            this.attributes.putAll(prototype.attributes);
            this.properties.addAll(prototype.properties);
        }

        /**
         * Constructs a configuration model based on the json object.
         *
         * @param jsonObject a json object with the configuration model.
         */
        public Builder(final JsonObject jsonObject) {
            final JsonObject prototype = Objects.requireNonNull(jsonObject);
            final Object id = prototype.get("id");
            this.id = id != null ? ((BigDecimal) id).intValue() : 0;
            this.name = Objects.requireNonNull((String) prototype.get("name"));
            this.description = (String) prototype.get("description");
            final Object version = prototype.get("version");
            this.version = version != null ? ((BigDecimal) version).intValue() : 1;
            final Object updated = prototype.get("updated");
            this.updated = updated != null ? ((BigDecimal) updated).longValue() : Clock.systemDefaultZone().millis();
            getAttributes(prototype).ifPresent(this.attributes::putAll);
            this.properties.addAll(getProperties(prototype).collect(Collectors.toList()));
        }

        /**
         * Constructs a configuration model with required parameters.
         *
         * @param name       a configuration name.
         * @param properties configuration properties.
         */
        public Builder(final String name, final Collection<Property> properties) {
            this.id = 0;
            this.name = Objects.requireNonNull(name);
            this.version = 1;
            this.updated = Clock.systemDefaultZone().millis();
            this.properties.addAll(Objects.requireNonNull(properties));
        }

        /**
         * Constructs a configuration model with the id parameter.
         *
         * @param id a configuration id.
         * @return a builder of the configuration model.
         */
        public Builder id(final int id) {
            if (id > 0) {
                this.id = id;
            } else {
                throw new IllegalArgumentException(WRONG_ID_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration model with the description parameter.
         *
         * @param description a configuration description.
         * @return a builder of the configuration model.
         */
        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Constructs a configuration model with the version parameter.
         *
         * @param version a configuration version.
         * @return a builder of the configuration model.
         */
        public Builder version(final int version) {
            if (version > 0) {
                this.version = version;
            } else {
                throw new IllegalArgumentException(WRONG_VERSION_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration model with the updated parameter.
         *
         * @param updated a configuration updated.
         * @return a builder of the configuration model.
         */
        public Builder updated(final long updated) {
            if (updated > 0) {
                this.updated = updated;
            } else {
                throw new IllegalArgumentException(WRONG_UPDATED_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration model with an attribute.
         *
         * @param key   a key of the attribute.
         * @param value a value of the attribute.
         * @return a builder of the configuration model.
         */
        public Builder attribute(final String key, final String value) {
            this.attributes.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        /**
         * Constructs a configuration model with attributes.
         *
         * @param attributes configuration attributes.
         * @return a builder of the configuration model.
         */
        public Builder attributes(final Map<String, String> attributes) {
            this.attributes.putAll(Objects.requireNonNull(attributes));
            return this;
        }

        /**
         * Constructs a configuration model with a property.
         *
         * @param paths    paths to a property.
         * @param property a configuration property.
         * @return a builder of the configuration model.
         */
        public Builder property(final String[] paths, final Property property) {
            return properties(paths, Collections.singletonList(Objects.requireNonNull(property)));
        }

        /**
         * Constructs a configuration model with properties.
         *
         * @param paths      path to a properties.
         * @param properties configuration properties.
         * @return a builder of the configuration model.
         */
        public Builder properties(final String[] paths, final Collection<Property> properties) {
            final String[] propertyPaths = Objects.requireNonNull(paths);
            if (propertyPaths.length > 0) {
                // todo implement additing properties
//                addAll(paths, 0, this.properties, properties);
            } else {
                this.properties.addAll(Objects.requireNonNull(properties));
            }

            return this;
        }

        /**
         * Builds a configuration model with required parameters.
         *
         * @return a builder of the configuration model.
         */
        public Config build() {
            return new Config(this);
        }
    }
}

