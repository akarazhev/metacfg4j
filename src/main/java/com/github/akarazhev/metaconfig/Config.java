package com.github.akarazhev.metaconfig;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class Config {
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

    public final static class Builder {
        private final String name;
        private String description;
        private final long created;
        private final long updated;
        private Map<String, String> attributes;
        private final Collection<Property> properties;

        public Builder(final String name, final Collection<Property> properties) {
            this.name = Objects.requireNonNull(name);
            this.created = Clock.systemDefaultZone().millis();
            this.updated = created;
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

    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }
}

