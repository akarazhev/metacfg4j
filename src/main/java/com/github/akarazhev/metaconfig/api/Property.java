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
 * The property model that contains parameters, attributes and properties.
 */
public final class Property implements Configurable {
    private final String name;
    private final String caption;
    private final String description;
    private final Type type;
    private final String value;
    private final int version;
    private final Map<String, String> attributes;
    private final Collection<Property> properties;

    /**
     * Types of a stored property value.
     */
    public static enum Type {
        BOOLEAN,
        DOUBLE,
        LONG,
        STRING,
        STRING_ARRAY
    }

    private Property(final Builder builder) {
        this.name = builder.name;
        this.caption = builder.caption;
        this.description = builder.description;
        this.type = builder.type;
        this.value = builder.value;
        this.version = builder.version;
        this.attributes = builder.attributes;
        this.properties = builder.properties;
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
    public String getCaption() {
        return caption;
    }

    /**
     * Returns a description of the property (optional).
     *
     * @return a property description.
     */
    public String getDescription() {
        return description;
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
     * Returns a value of the property.
     *
     * @return a property value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a version of the property.
     *
     * @return a version value.
     */
    public int getVersion() {
        return version;
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
        json.put("caption", caption);
        json.put("description", description);
        json.put("type", type.name());
        json.put("value", value);
        json.put("version", version);
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
        final Property property = (Property) o;
        return name.equals(property.name) &&
                Objects.equals(caption, property.caption) &&
                Objects.equals(description, property.description) &&
                type == property.type &&
                value.equals(property.value) &&
                version == property.version &&
                Objects.equals(attributes, property.attributes) &&
                Objects.equals(properties, property.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, caption, description, type, value, version, attributes, properties);
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
                ", version=" + version +
                '}';
    }

    /**
     * Wraps and builds the instance of the property model.
     */
    public final static class Builder extends ConfigBuilder {
        private final String name;
        private String caption;
        private String description;
        private final Type type;
        private final String value;
        private final int version;

        /**
         * Constructs a property model based on the json object.
         *
         * @param jsonObject a json object with the property model.
         */
        public Builder(final JsonObject jsonObject) {
            this.name = Objects.requireNonNull((String) jsonObject.get("name"));
            this.caption = (String) jsonObject.get("caption");
            this.description = (String) jsonObject.get("description");
            this.type = Type.valueOf(Objects.requireNonNull((String) jsonObject.get("type")));
            this.value = Objects.requireNonNull((String) jsonObject.get("value"));
            this.version = Objects.requireNonNull((BigDecimal) jsonObject.get("version")).intValue();
            getAttributes(jsonObject.get("attributes")).ifPresent(attributes -> this.attributes = attributes);
            this.properties = getProperties(jsonObject.get("properties")).collect(Collectors.toList());
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a property value.
         */
        public Builder(final String name, final String value) {
            this.name = Objects.requireNonNull(name);
            this.type = Type.STRING;
            this.value = Objects.requireNonNull(value);
            this.version = 1;
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param type  a property type.
         * @param value a property value.
         */
        public Builder(final String name, final Type type, final String value) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
            this.value = Objects.requireNonNull(value);
            this.version = 1;
        }

        /**
         * Constructs a property model with the caption parameter.
         *
         * @param caption a property caption.
         * @return a builder of the property model.
         */
        public Builder caption(final String caption) {
            this.caption = Objects.requireNonNull(caption);
            return this;
        }

        /**
         * Constructs a property model with the description parameter.
         *
         * @param description a property description.
         * @return a builder of the property model.
         */
        public Builder description(final String description) {
            this.description = Objects.requireNonNull(description);
            return this;
        }

        /**
         * Constructs a property model with attributes.
         *
         * @param attributes property attributes.
         * @return a builder of the property model.
         */
        public Builder attributes(final Map<String, String> attributes) {
            this.attributes = new HashMap<>(Objects.requireNonNull(attributes));
            return this;
        }

        /**
         * Constructs a property model with properties.
         *
         * @param properties property properties.
         * @return a builder of the property model.
         */
        public Builder properties(final Collection<Property> properties) {
            this.properties = new ArrayList<>(Objects.requireNonNull(properties));
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
