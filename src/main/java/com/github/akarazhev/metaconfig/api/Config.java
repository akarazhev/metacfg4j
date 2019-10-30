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

/**
 * The configuration model that contains parameters, attributes and properties.
 */
public final class Config implements Configurable {
    private final String name;
    private final String description;
    private final int version;
    private final long updated;
    private final Map<String, String> attributes;
    private final Collection<Property> properties;

    private Config(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.updated = builder.updated;
        this.attributes = builder.attributes;
        this.properties = builder.properties;
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
        return attributes != null ?
                Optional.of(Collections.unmodifiableMap(attributes)) :
                Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getAttributeKeys() {
        return attributes != null ?
                attributes.keySet().stream() :
                Stream.empty();
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
    public Optional<Property> getProperty(final String name) {
        return properties.stream().filter(property -> property.getName().equals(name)).findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Property> getProperty(final String[] paths, final String name) {
        // todo is not implemented
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(final Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
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
        final Config config = (Config) o;
        return version == config.version &&
                updated == config.updated &&
                name.equals(config.name) &&
                Objects.equals(description, config.description) &&
                Objects.equals(attributes, config.attributes) &&
                properties.equals(config.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, description, version, updated, attributes, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", version=" + version +
                ", updated=" + updated +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration model.
     */
    public final static class Builder extends ConfigBuilder {
        private final String name;
        private String description;
        private final int version;
        private final long updated;

        /**
         * Constructs a configuration model based on the json object.
         *
         * @param jsonObject a json object with the configuration model.
         */
        public Builder(final JsonObject jsonObject) {
            this.name = Objects.requireNonNull((String) jsonObject.get("name"));
            this.description = (String) jsonObject.get("description");
            this.version = Objects.requireNonNull((BigDecimal) jsonObject.get("version")).intValue();
            this.updated = Objects.requireNonNull((BigDecimal) jsonObject.get("updated")).longValue();
            getAttributes(jsonObject.get("attributes")).ifPresent(this.attributes::putAll);
            this.properties.addAll(getProperties(jsonObject.get("properties")).collect(Collectors.toList()));
        }

        /**
         * Constructs a configuration model with required parameters.
         *
         * @param name       a configuration name.
         * @param properties configuration properties.
         */
        public Builder(final String name, final Collection<Property> properties) {
            this.name = Objects.requireNonNull(name);
            this.version = 1;
            this.updated = Clock.systemDefaultZone().millis();
            this.properties.addAll(Objects.requireNonNull(properties));
        }

        /**
         * Constructs a configuration model with the description parameter.
         *
         * @param description a configuration description.
         * @return a builder of the configuration model.
         */
        public Builder description(final String description) {
            this.description = Objects.requireNonNull(description);
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
            // todo is not implemented
            return this;
        }

        /**
         * Constructs a configuration model with properties.
         *
         * @param paths      path to a properties.
         * @param properties configuration properties.
         * @return a builder of the configuration model.
         */
        public Builder properties(final String[] paths, final Collection<Property> properties) {
            // todo is not implemented
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

