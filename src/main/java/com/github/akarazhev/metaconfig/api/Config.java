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
import com.github.cliftonlabs.json_simple.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.*;
import static com.github.akarazhev.metaconfig.api.Configurable.ConfigBuilder.*;

/**
 * The configuration model that contains parameters, attributes and properties.
 */
public final class Config implements Configurable {
    private final long id;
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
    public long getId() {
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
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
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
        return Optional.ofNullable(attributes.get(Validator.of(key).get()));
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
        return Configurable.getProperty(0, paths, getProperties());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(final Writer writer) throws IOException {
        final var json = new JsonObject();
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var config = (Config) o;
        return id == config.id &&
                version == config.version &&
                updated == config.updated &&
                Objects.equals(name, config.name) &&
                Objects.equals(description, config.description) &&
                Objects.equals(attributes, config.attributes) &&
                Objects.equals(properties, config.properties);
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
                ", description='" + description + '\'' +
                ", version=" + version +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration model.
     */
    public final static class Builder {
        private final Map<String, String> attributes = new HashMap<>();
        private final String name;
        private final Collection<Property> properties = new LinkedList<>();
        private long id = 0;
        private int version = 1;
        private long updated = Clock.systemDefaultZone().millis();
        private String description;

        /**
         * Constructs a configuration model based on the config object.
         *
         * @param config a configuration model.
         */
        public Builder(final Config config) {
            final var prototype = Validator.of(config).get();
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
            final var prototype = Validator.of(jsonObject).get();
            this.id = getLong(prototype, "id");
            this.name = Validator.of((String) prototype.get("name")).get();
            this.description = (String) prototype.get("description");
            final var version = getLong(prototype, "version");
            if (version > 0) {
                this.version = (int) version;
            }
            this.updated = getLong(prototype, "updated");
            ConfigBuilder.getAttributes(prototype).ifPresent(this.attributes::putAll);
            this.properties.addAll(ConfigBuilder.getProperties(prototype).collect(Collectors.toList()));
        }

        /**
         * Constructs a configuration model with required parameters.
         *
         * @param name       a configuration name.
         * @param properties configuration properties.
         */
        public Builder(final String name, final Collection<Property> properties) {
            this.name = Validator.of(name).get();
            this.properties.addAll(Validator.of(properties).get());
        }

        /**
         * Constructs a configuration model with the id parameter.
         *
         * @param id a configuration id.
         * @return a builder of the configuration model.
         */
        public Builder id(final long id) {
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
            this.attributes.put(Validator.of(key).get(), Validator.of(value).get());
            return this;
        }

        /**
         * Constructs a configuration model with attributes.
         *
         * @param attributes configuration attributes.
         * @return a builder of the configuration model.
         */
        public Builder attributes(final Map<String, String> attributes) {
            this.attributes.putAll(Validator.of(attributes).get());
            return this;
        }

        /**
         * Constructs a configuration model without a property by paths.
         *
         * @param paths paths to a configuration property.
         * @return a builder of the configuration model.
         */
        public Builder deleteProperty(final String[] paths) {
            properties(deleteProperties(paths, this.properties.stream()));
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
            return properties(paths, Collections.singletonList(Validator.of(property).get()));
        }

        /**
         * Constructs a configuration model with properties.
         *
         * @param paths      path to a properties.
         * @param properties configuration properties.
         * @return a builder of the configuration model.
         */
        public Builder properties(final String[] paths, final Collection<Property> properties) {
            setProperties(this.properties, paths, properties);
            return this;
        }

        /**
         * Constructs a configuration model with properties.
         * It replaces all properties.
         *
         * @param properties configuration properties.
         * @return a builder of the configuration model.
         */
        public Builder properties(final Collection<Property> properties) {
            this.properties.clear();
            this.properties.addAll(Validator.of(properties).get());
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

