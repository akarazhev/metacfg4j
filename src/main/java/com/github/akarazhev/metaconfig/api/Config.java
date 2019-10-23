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
import java.util.stream.Stream;

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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCreated() {
        return created;
    }

    public long getUpdated() {
        return updated;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Stream<Property> getProperties() {
        return properties.stream();
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("name", name);
        json.put("description", description);
        json.put("created", created);
        json.put("updated", updated);
        json.put("attributes", attributes);
        json.put("properties", properties);
        json.toJson(writer);
    }

    @Override
    public boolean equals(Object o) {
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

    @Override
    public int hashCode() {
        return Objects.hash(name, description, created, updated, attributes, properties);
    }

    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    public final static class Builder {
        private final String name;
        private String description;
        private final long created;
        private final long updated;
        private Map<String, String> attributes;
        private final Collection<Property> properties;

        public Builder(final JsonObject jsonObject) {
            this.name = Objects.requireNonNull((String) jsonObject.get("name"));
            this.description = (String) jsonObject.get("description");
            this.created = Objects.requireNonNull((BigDecimal) jsonObject.get("created")).longValue();
            this.updated = Objects.requireNonNull((BigDecimal) jsonObject.get("updated")).longValue();
            this.attributes = null; // todo
            this.properties = null;
        }

        public Builder(final String name, final Collection<Property> properties) {
            this.name = Objects.requireNonNull(name);
            final long millis = Clock.systemDefaultZone().millis();
            this.created = millis;
            this.updated = millis;
            this.properties = new ArrayList<>(Objects.requireNonNull(properties));
        }

        public Builder description(final String description) {
            this.description = Objects.requireNonNull(description);
            return this;
        }

        public Builder attributes(final Map<String, String> attributes) {
            this.attributes = new HashMap<>(Objects.requireNonNull(attributes));
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}

