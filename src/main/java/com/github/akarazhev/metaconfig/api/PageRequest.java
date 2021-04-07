/* Copyright 2019-2021 Andrey Karazhev
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

import com.github.akarazhev.metaconfig.extension.ExtJsonable;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.akarazhev.metaconfig.Constants.Messages.EMPTY_ASCENDING_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_PAGE_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_SIZE_VALUE;
import static com.github.akarazhev.metaconfig.api.Configurable.ConfigBuilder.getLong;

/**
 * The configuration page request model that contains a name, attributes, page number, size and sorting property.
 */
public final class PageRequest implements ExtJsonable {
    private final String name;
    private final Map<String, String> attributes;
    private final int page;
    private final int size;
    private final boolean ascending;

    private PageRequest(final Builder builder) {
        this.name = builder.name;
        this.attributes = builder.attributes;
        this.page = builder.page;
        this.size = builder.size;
        this.ascending = builder.ascending;
    }

    /**
     * Returns a configuration name.
     *
     * @return a configuration name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns configuration attributes.
     *
     * @return attributes as a map.
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns a page number.
     *
     * @return a page number.
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns a size of instances.
     *
     * @return a size of instances.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns a sorting property.
     *
     * @return true for ascending sorting.
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("name", name);
        json.put("attributes", attributes);
        json.put("page", page);
        json.put("size", size);
        json.put("ascending", ascending);
        json.toJson(writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "PageRequest{" +
                "name='" + name + '\'' +
                ", attributes=" + attributes +
                ", page=" + page +
                ", size=" + size +
                ", ascending=" + ascending +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration page request model.
     */
    public final static class Builder {
        private final String name;
        private final Map<String, String> attributes = new HashMap<>();
        private int page = 0;
        private int size = Integer.MAX_VALUE;
        private boolean ascending = true;

        /**
         * Constructs a configuration page request model based on the json object.
         *
         * @param jsonObject a json object with the configuration page request model.
         */
        public Builder(final JsonObject jsonObject) {
            final JsonObject prototype = Validator.of(jsonObject).get();
            this.name = Validator.of((String) prototype.get("name")).get();
            Configurable.ConfigBuilder.getAttributes(prototype).ifPresent(this.attributes::putAll);
            final long page = getLong(prototype, "page");
            if (page >= 0) {
                this.page = (int) page;
            } else {
                throw new IllegalArgumentException(WRONG_PAGE_VALUE);
            }
            final long size = getLong(prototype, "size");
            if (size >= 0) {
                this.size = (int) size;
            } else {
                throw new IllegalArgumentException(WRONG_SIZE_VALUE);
            }
            final Object value = jsonObject.get("ascending");
            if (value != null) {
                this.ascending = (Boolean) value;
            } else {
                throw new IllegalArgumentException(EMPTY_ASCENDING_VALUE);
            }
        }

        /**
         * Constructs a configuration page request model with the required name parameter.
         *
         * @param name a configuration name.
         */
        public Builder(final String name) {
            this.name = Validator.of(name).get();
        }

        /**
         * Constructs a configuration page request model with an attribute.
         *
         * @param key   a key of the attribute.
         * @param value a value of the attribute.
         * @return a builder of the configuration page request model.
         */
        public Builder attribute(final String key, final String value) {
            this.attributes.put(Validator.of(key).get(), Validator.of(value).get());
            return this;
        }

        /**
         * Constructs a configuration page request model with attributes.
         *
         * @param attributes configuration attributes.
         * @return a builder of the configuration page request model.
         */
        public Builder attributes(final Map<String, String> attributes) {
            this.attributes.putAll(Validator.of(attributes).get());
            return this;
        }

        /**
         * Constructs a configuration page request model with a page number.
         *
         * @param page a page number.
         * @return a builder of the configuration page request model.
         */
        public Builder page(final int page) {
            if (page >= 0) {
                this.page = page;
            } else {
                throw new IllegalArgumentException(WRONG_PAGE_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration page request model with a size.
         *
         * @param size a size of instances.
         * @return a builder of the configuration page request model.
         */
        public Builder size(final int size) {
            if (size >= 0) {
                this.size = size;
            } else {
                throw new IllegalArgumentException(WRONG_SIZE_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration page request model with a sorting property.
         *
         * @param ascending a sorting property.
         * @return a builder of the configuration page request model.
         */
        public Builder ascending(final boolean ascending) {
            this.ascending = ascending;
            return this;
        }

        /**
         * Builds a configuration page request model with required parameters.
         *
         * @return a builder of the configuration page request model.
         */
        public PageRequest build() {
            return new PageRequest(this);
        }
    }
}