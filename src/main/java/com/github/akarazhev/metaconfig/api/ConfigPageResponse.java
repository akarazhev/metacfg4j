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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_PAGE_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_TOTAL_VALUE;

/**
 * The configuration page response that contains a page number, total and names.
 */
public final class ConfigPageResponse {
    private final int page;
    private final int total;
    private final Collection<String> names;

    private ConfigPageResponse(final Builder builder) {
        this.page = builder.page;
        this.total = builder.total;
        this.names = builder.names;
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
     * Returns a total of instances.
     *
     * @return a total of instances.
     */
    public int getTotal() {
        return total;
    }

    /**
     * Returns names of configs.
     *
     * @return names of instances as a stream.
     */
    public Stream<String> getStream() {
        return names.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ConfigPageResponse response = (ConfigPageResponse) o;
        return page == response.page &&
                total == response.total &&
                names.equals(response.names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(page, total, names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConfigPage{" +
                "page=" + page +
                ", total=" + total +
                ", names=" + names +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration page response model.
     */
    final static class Builder {
        private final Collection<String> names = new LinkedList<>();
        private int page = 0;
        private int total = 0;

        /**
         * Constructs a configuration page response model with a page number.
         *
         * @param page a page number.
         * @return a builder of the configuration page response model.
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
         * Constructs a configuration page response model with a total.
         *
         * @param total a total of instances.
         * @return a builder of the configuration page response model.
         */
        public Builder total(final int total) {
            if (total >= 0) {
                this.total = total;
            } else {
                throw new IllegalArgumentException(WRONG_TOTAL_VALUE);
            }

            return this;
        }

        /**
         * Constructs a configuration page response model with names.
         *
         * @param names configuration names.
         * @return a builder of the configuration page response model.
         */
        public Builder names(final Collection<String> names) {
            this.names.addAll(Validator.of(names).get());
            return this;
        }

        /**
         * Builds a configuration page response model with required parameters.
         *
         * @return a builder of the configuration page response model.
         */
        public ConfigPageResponse build() {
            return new ConfigPageResponse(this);
        }
    }
}