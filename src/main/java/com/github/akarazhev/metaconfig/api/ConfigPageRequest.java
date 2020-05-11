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

import com.github.akarazhev.metaconfig.extension.Validator;

import java.util.Objects;

import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_PAGE_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_SIZE_VALUE;

/**
 * The configuration page request model that contains a name, page number, size and sorting property.
 */
public final class ConfigPageRequest {
    private final String name;
    private final int page;
    private final int size;
    private final boolean ascending;

    private ConfigPageRequest(final Builder builder) {
        this.name = builder.name;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ConfigPageRequest request = (ConfigPageRequest) o;
        return page == request.page &&
                size == request.size &&
                ascending == request.ascending &&
                name.equals(request.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, page, size, ascending);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConfigPageRequest{" +
                "name='" + name + '\'' +
                ", page=" + page +
                ", size=" + size +
                ", ascending=" + ascending +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration page request model.
     */
    final static class Builder {
        private final String name;
        private int page = 0;
        private int size = Integer.MAX_VALUE;
        private boolean ascending = true;

        /**
         * Constructs a configuration page request model with the required name parameter.
         *
         * @param name a configuration name.
         */
        public Builder(final String name) {
            this.name = Validator.of(name).get();
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
        public ConfigPageRequest build() {
            return new ConfigPageRequest(this);
        }
    }
}