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
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.IOException;
import java.io.Writer;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_ID_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_UPDATED_VALUE;
import static com.github.akarazhev.metaconfig.api.Configurable.ConfigBuilder.*;

/**
 * The property model that contains parameters, attributes and properties.
 */
public final class Property implements Configurable {
    private final long id;
    private final String name;
    private final String caption;
    private final String description;
    private final Type type;
    private final String value;
    private final long updated;
    private final Map<String, String> attributes;
    private final Collection<Property> properties;

    public enum Type {
        BOOL,
        DOUBLE,
        LONG,
        STRING,
        STRING_ARRAY
    }

    private Property(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.caption = builder.caption;
        this.description = builder.description;
        this.type = builder.type;
        this.value = builder.value;
        this.updated = builder.updated;
        this.attributes = builder.attributes;
        this.properties = builder.properties;
    }

    /**
     * Returns an id of the property.
     *
     * @return a property id.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns a name of the property.
     *
     * @return a property name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a caption of the property (optional).
     *
     * @return a property caption.
     */
    public Optional<String> getCaption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns a description of the property (optional).
     *
     * @return a property description.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns a value of the property.
     *
     * @return a property value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a boolean value of the property.
     *
     * @return a property value.
     */
    public boolean asBool() {
        if (Type.BOOL.equals(type)) {
            return Boolean.parseBoolean(value);
        }

        throw new ClassCastException("Property has the different type: " + type);
    }

    /**
     * Returns a double value of the property.
     *
     * @return a property value.
     */
    public double asDouble() {
        if (Type.DOUBLE.equals(type)) {
            return Double.parseDouble(value);
        }

        throw new ClassCastException("Property has the different type: " + type);
    }

    /**
     * Returns a long value of the property.
     *
     * @return a property value.
     */
    public long asLong() {
        if (Type.LONG.equals(type)) {
            return Long.parseLong(value);
        }

        throw new ClassCastException("Property has the different type: " + type);
    }

    /**
     * Returns an array value of the property.
     *
     * @return a property value.
     */
    public String[] asArray() {
        if (Type.STRING_ARRAY.equals(type)) {
            return Jsoner.deserialize(value, new JsonArray()).stream().
                    map(Objects::toString).
                    toArray(String[]::new);
        }

        throw new ClassCastException("Property has the different type: " + type);
    }

    /**
     * Returns a type of the property.
     *
     * @return a property type.
     */
    public Type getType() {
        return type;
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
        json.put("caption", caption);
        json.put("description", description);
        json.put("type", type.name());
        json.put("value", value);
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
        final var property = (Property) o;
        return id == property.id &&
                updated == property.updated &&
                Objects.equals(name, property.name) &&
                Objects.equals(caption, property.caption) &&
                Objects.equals(description, property.description) &&
                Objects.equals(type, property.type) &&
                Objects.equals(value, property.value) &&
                Objects.equals(attributes, property.attributes) &&
                Objects.equals(properties, property.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, caption, description, type, value, updated, attributes, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", caption='" + caption + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", value='" + value + '\'' +
                '}';
    }

    /**
     * It is only used by the builder and must not be used directly.
     *
     * @return a reference to properties.
     */
    Collection<Property> properties() {
        return properties;
    }

    /**
     * Wraps and builds the instance of the property model.
     */
    public final static class Builder {
        private final Map<String, String> attributes = new HashMap<>();
        private final String name;
        private final Collection<Property> properties = new LinkedList<>();
        private long id = 0;
        private final Type type;
        private final String value;
        private long updated = Clock.systemDefaultZone().millis();
        private String caption;
        private String description;

        /**
         * Constructs a property model based on the property object.
         *
         * @param property a property model.
         */
        public Builder(final Property property) {
            final var prototype = Validator.of(property).get();
            this.id = prototype.id;
            this.name = prototype.name;
            this.caption = prototype.caption;
            this.description = prototype.description;
            this.type = prototype.type;
            this.value = prototype.value;
            this.updated = prototype.updated;
            this.attributes.putAll(prototype.attributes);
            this.properties.addAll(prototype.properties);
        }

        /**
         * Constructs a property model based on the json object.
         *
         * @param jsonObject a json object with the property model.
         */
        public Builder(final JsonObject jsonObject) {
            final var prototype = Validator.of(jsonObject).get();
            this.id = getLong(prototype, "id");
            this.name = Validator.of((String) prototype.get("name")).get();
            this.caption = (String) prototype.get("caption");
            this.description = (String) prototype.get("description");
            this.type = Type.valueOf(Validator.of((String) prototype.get("type")).get());
            this.value = Validator.of((String) prototype.get("value")).get();
            this.updated = getLong(prototype, "updated");
            ConfigBuilder.getAttributes(prototype).ifPresent(this.attributes::putAll);
            this.properties.addAll(ConfigBuilder.getProperties(prototype).collect(Collectors.toList()));
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a string property value.
         */
        public Builder(final String name, final String value) {
            this.name = Validator.of(name).get();
            this.type = Type.STRING;
            this.value = Validator.of(value).get();
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param type  a property type.
         * @param value a property value.
         */
        public Builder(final String name, final String type, final String value) {
            this.name = Validator.of(name).get();
            this.type = Type.valueOf(Validator.of(type).get());
            this.value = Validator.of(value).get();
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a boolean property value.
         */
        public Builder(final String name, final boolean value) {
            this.name = Validator.of(name).get();
            this.type = Type.BOOL;
            this.value = String.valueOf(value);
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a double property value.
         */
        public Builder(final String name, final double value) {
            this.name = Validator.of(name).get();
            this.type = Type.DOUBLE;
            this.value = String.valueOf(value);
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a long property value.
         */
        public Builder(final String name, final long value) {
            this.name = Validator.of(name).get();
            this.type = Type.LONG;
            this.value = String.valueOf(value);
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value an array property value.
         */
        public Builder(final String name, final String... value) {
            this.name = Validator.of(name).get();
            this.type = Type.STRING_ARRAY;
            this.value = new JsonArray(Arrays.asList(Validator.of(value).get())).toJson();
        }

        /**
         * Constructs a property model with the id parameter.
         *
         * @param id a property id.
         * @return a builder of the property model.
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
         * Constructs a property model with the caption parameter.
         *
         * @param caption a property caption.
         * @return a builder of the property model.
         */
        public Builder caption(final String caption) {
            this.caption = caption;
            return this;
        }

        /**
         * Constructs a property model with the description parameter.
         *
         * @param description a property description.
         * @return a builder of the property model.
         */
        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Constructs a property model with the updated parameter.
         *
         * @param updated a property updated.
         * @return a builder of the property model.
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
         * Constructs a property model with an attribute.
         *
         * @param key   a key of the attribute.
         * @param value a value of the attribute.
         * @return a builder of the property model.
         */
        public Builder attribute(final String key, final String value) {
            this.attributes.put(Validator.of(key).get(), Validator.of(value).get());
            return this;
        }

        /**
         * Constructs a property model with attributes.
         *
         * @param attributes property attributes.
         * @return a builder of the property model.
         */
        public Builder attributes(final Map<String, String> attributes) {
            this.attributes.putAll(Validator.of(attributes).get());
            return this;
        }

        /**
         * Constructs a property model without a property by paths.
         *
         * @param paths paths to a property.
         * @return a builder of the property model.
         */
        public Builder deleteProperty(final String[] paths) {
            properties(deleteProperties(paths, this.properties.stream()));
            return this;
        }

        /**
         * Constructs a property model with a property.
         *
         * @param paths    paths to a property.
         * @param property a property property.
         * @return a builder of the property model.
         */
        public Builder property(final String[] paths, final Property property) {
            return properties(paths, Collections.singletonList(Validator.of(property).get()));
        }

        /**
         * Constructs a property model with properties.
         *
         * @param paths      paths to a properties.
         * @param properties property properties.
         * @return a builder of the property model.
         */
        public Builder properties(final String[] paths, final Collection<Property> properties) {
            setProperties(this.properties, paths, properties);
            return this;
        }

        /**
         * Constructs a property model with properties.
         * It replaces all properties.
         *
         * @param properties property properties.
         * @return a builder of the property model.
         */
        public Builder properties(final Collection<Property> properties) {
            this.properties.clear();
            this.properties.addAll(Validator.of(properties).get());
            return this;
        }

        /**
         * Builds a property model with required parameters.
         *
         * @return a builder of the property model.
         */
        public Property build() {
            return new Property(this);
        }
    }
}
