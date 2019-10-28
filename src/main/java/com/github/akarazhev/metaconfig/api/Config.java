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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
    private final long created;
    private final long updated;
    private final Map<String, String> attributes;
    private final Collection<Property> properties;

    private Config(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.created = builder.created;
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
     * Returns a creation time of the configuration.
     *
     * @return a configuration created time value.
     */
    public long getCreated() {
        return created;
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
    public void toJson(final Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("name", name);
        json.put("description", description);
        json.put("created", created);
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
        return created == config.created &&
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
        return Objects.hash(name, description, created, updated, attributes, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration model.
     */
    public final static class Builder extends ConfigBuilder {
        private final String name;
        private String description;
        private final long created;
        private final long updated;
        private Map<String, String> attributes;
        private Collection<Property> properties;

        /**
         * Constructs a configuration model based on the json object.
         *
         * @param jsonObject a json object with the configuration model.
         */
        public Builder(final JsonObject jsonObject) {
            this.name = Objects.requireNonNull((String) jsonObject.get("name"));
            this.description = (String) jsonObject.get("description");
            this.created = Objects.requireNonNull((BigDecimal) jsonObject.get("created")).longValue();
            this.updated = Objects.requireNonNull((BigDecimal) jsonObject.get("updated")).longValue();
            getAttributes(jsonObject.get("attributes")).ifPresent(attributes -> this.attributes = attributes);
            this.properties = getProperties(jsonObject.get("properties")).collect(Collectors.toList());
        }

        /**
         * Constructs a configuration model with required parameters.
         *
         * @param name       a configuration name.
         * @param properties configuration properties.
         */
        public Builder(final String name, final Collection<Property> properties) {
            this.name = Objects.requireNonNull(name);
            final long millis = Clock.systemDefaultZone().millis();
            this.created = millis;
            this.updated = millis;
            this.properties = new ArrayList<>(Objects.requireNonNull(properties));
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
            this.attributes = new HashMap<>(Objects.requireNonNull(attributes));
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

