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

import com.github.akarazhev.metaconfig.extension.ExtJsonable;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_PAGE_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_TOTAL_VALUE;
import static com.github.akarazhev.metaconfig.api.Configurable.ConfigBuilder.getLong;

/**
 * The configuration page response that contains a page number, total and names.
 */
public final class PageResponse implements ExtJsonable {
    private final int page;
    private final int total;
    private final Collection<String> names;

    private PageResponse(final Builder builder) {
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
    public Stream<String> getNames() {
        return names.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("page", page);
        json.put("total", total);
        json.put("names", names);
        json.toJson(writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "PageResponse{" +
                "page=" + page +
                ", total=" + total +
                ", names=" + names +
                '}';
    }

    /**
     * Wraps and builds the instance of the configuration page response model.
     */
    public final static class Builder {
        private final Collection<String> names = new LinkedList<>();
        private int page = 0;
        private int total = 0;

        /**
         * Constructs a configuration page response model with names.
         *
         * @param names configuration names.
         */
        public Builder(final Collection<String> names) {
            this.names.addAll(Validator.of(names).get());
        }

        /**
         * Constructs a configuration page response model based on the json object.
         *
         * @param jsonObject a json object with the configuration page response model.
         */
        public Builder(final JsonObject jsonObject) {
            final JsonObject prototype = Validator.of(jsonObject).get();
            final JsonArray jsonNames = (JsonArray) prototype.get("names");
            if (jsonNames != null) {
                names.addAll(jsonNames.stream().map(Object::toString).collect(Collectors.toList()));
            }
            final long page = getLong(prototype, "page");
            if (page >= 0) {
                this.page = (int) page;
            } else {
                throw new IllegalArgumentException(WRONG_PAGE_VALUE);
            }
            final long total = getLong(prototype, "total");
            if (total >= 0) {
                this.total = (int) total;
            } else {
                throw new IllegalArgumentException(WRONG_TOTAL_VALUE);
            }
        }

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
         * Builds a configuration page response model with required parameters.
         *
         * @return a builder of the configuration page response model.
         */
        public PageResponse build() {
            return new PageResponse(this);
        }
    }
}