package com.github.akarazhev.metaconfig.api;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class Config implements Jsonable {
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

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Collection<Property> getProperties() {
        return properties;
    }

    @Override
    public String toJson() {
        final StringWriter writable = new StringWriter();
        try {
            toJson(writable);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return writable.toString();
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("name", getName());
        json.put("description", getDescription());
        json.put("created", getCreated());
        json.put("updated", getUpdated());
        json.put("attributes", getAttributes());
        json.put("properties", getProperties());
        json.toJson(writer);
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

        public Builder(final String name, final Collection<Property> properties) {
            long millis = Clock.systemDefaultZone().millis();
            this.name = Objects.requireNonNull(name);
            this.created = millis;
            this.updated = millis;
            this.properties = Collections.unmodifiableCollection(Objects.requireNonNull(properties));
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder attributes(final Map<String, String> attributes) {
            this.attributes = Collections.unmodifiableMap(Objects.requireNonNull(attributes));
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}

