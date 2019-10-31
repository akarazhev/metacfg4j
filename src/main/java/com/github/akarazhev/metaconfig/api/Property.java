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

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    private enum Type {
        BOOL,
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
            return Boolean.valueOf(value);
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
            return Double.valueOf(value);
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
            return Long.valueOf(value);
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
                    map(Object::toString).
                    toArray(String[]::new);
        }

        throw new ClassCastException("Property has the different type: " + type);
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
        return Configurable.getProperty(0, paths, getProperties());
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
            getAttributes(jsonObject.get("attributes")).ifPresent(this.attributes::putAll);
            this.properties.addAll(getProperties(jsonObject.get("properties")).collect(Collectors.toList()));
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a string property value.
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
         * @param value a boolean property value.
         */
        public Builder(final String name, final boolean value) {
            this.name = Objects.requireNonNull(name);
            this.type = Type.BOOL;
            this.value = String.valueOf(value);
            this.version = 1;
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a double property value.
         */
        public Builder(final String name, final double value) {
            this.name = Objects.requireNonNull(name);
            this.type = Type.DOUBLE;
            this.value = String.valueOf(value);
            this.version = 1;
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value a long property value.
         */
        public Builder(final String name, final long value) {
            this.name = Objects.requireNonNull(name);
            this.type = Type.LONG;
            this.value = String.valueOf(value);
            this.version = 1;
        }

        /**
         * Constructs a property model with required parameters.
         *
         * @param name  a property name.
         * @param value an array property value.
         */
        public Builder(final String name, final String... value) {
            this.name = Objects.requireNonNull(name);
            this.type = Type.STRING_ARRAY;
            this.value = new JsonArray(Arrays.asList(Objects.requireNonNull(value))).toJson();
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
            this.attributes.putAll(Objects.requireNonNull(attributes));
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
            return properties(paths, Collections.singletonList(Objects.requireNonNull(property)));
        }

        /**
         * Constructs a property model with properties.
         *
         * @param paths      paths to a properties.
         * @param properties property properties.
         * @return a builder of the property model.
         */
        public Builder properties(final String[] paths, final Collection<Property> properties) {
            final String[] propertyPaths = Objects.requireNonNull(paths);
            if (propertyPaths.length > 0) {
                addAll(0, paths, this.properties, properties);
            } else {
                this.properties.addAll(Objects.requireNonNull(properties));
            }

            return this;
        }

        /**
         * Constructs a property model with properties.
         *
         * @param properties property properties.
         * @return a builder of the property model.
         */
        public Builder properties(final Collection<Property> properties) {
            this.properties.addAll(Objects.requireNonNull(properties));
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

        private void addAll(final int index, final String[] paths, final Collection<Property> target,
                            final Collection<Property> source) {
            if (index < paths.length) {
                final int nextIndex = index + 1;
                final Optional<Property> currentProperty = target.stream().
                        filter(property -> paths[index].equals(property.getName())).findFirst();
                if (currentProperty.isPresent()) {
                    addAll(nextIndex, paths, currentProperty.get().properties, source);
                } else {
                    final Property newProperty = new Property.Builder(paths[index], "").build();
                    target.add(newProperty);
                    addAll(nextIndex, paths, newProperty.properties, source);
                }
            } else {
                target.addAll(source);
            }
        }
    }
}
