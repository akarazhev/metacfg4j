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

import java.util.stream.Stream;

/**
 * Provides repository methods to create, read, update and delete operations.
 */
interface ConfigRepository {
    /**
     * Returns configuration models for configuration names.
     *
     * @param stream a stream of names.
     * @return a stream of configurations models.
     */
    Stream<Config> findByNames(final Stream<String> stream);

    /**
     * Returns all configuration names.
     *
     * @return a stream of configuration names.
     */
    Stream<String> findNames();

    /**
     * Returns selected configuration names by a part of a name and a page request.
     *
     * @param name        a part of a name.
     * @param pageRequest a page request that has parameters: page, size, ascending.
     * @return a page with configuration names.
     */
    Page findByName(final String name, final PageRequest pageRequest);

    /**
     * Saves and flushes configuration models.
     *
     * @param stream a stream of configuration models.
     * @return a stream of updated configuration models.
     */
    Stream<Config> saveAndFlush(final Stream<Config> stream);

    /**
     * Deletes configuration models.
     *
     * @param stream a stream of names.
     * @return a number of deleted models.
     */
    int delete(final Stream<String> stream);

    /**
     * The page request model that contains a page number, size and sorting property.
     */
    final class PageRequest {
        private final int page;
        private final int size;
        private final boolean ascending;

        private PageRequest(final Builder builder) {
            this.page = builder.page;
            this.size = builder.size;
            this.ascending = builder.ascending;
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
         * Returns a size of instances on a page.
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
         * Wraps and builds the instance of the page request model.
         */
        final static class Builder {
            private int page = 0;
            private int size = Integer.MAX_VALUE;
            private boolean ascending = true;

            /**
             * Constructs a page request model with a page number.
             *
             * @param page a page number.
             * @return a builder of the page request model.
             */
            public Builder page(final int page) {
                this.page = page;
                return this;
            }

            /**
             * Constructs a page request model with a size.
             *
             * @param size a size of instances.
             * @return a builder of the page request model.
             */
            public Builder size(final int size) {
                this.size = size;
                return this;
            }

            /**
             * Constructs a page request model with a sorting property.
             *
             * @param ascending a sorting property.
             * @return a builder of the page request model.
             */
            public Builder ascending(final boolean ascending) {
                this.ascending = ascending;
                return this;
            }

            /**
             * Builds a page request model with required parameters.
             *
             * @return a builder of the page request model.
             */
            public PageRequest build() {
                return new PageRequest(this);
            }
        }
    }

    /**
     * The page model that contains a page number, total and stream of names.
     */
    final class Page {
        private final int page;
        private final int total;
        private final Stream<String> stream;

        private Page(final Builder builder) {
            this.page = builder.page;
            this.total = builder.total;
            this.stream = builder.stream;
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
         * Returns a total of instances on a page.
         *
         * @return a total of instances.
         */
        public int getTotal() {
            return total;
        }

        /**
         * Returns names of configs.
         *
         * @return names as a stream.
         */
        public Stream<String> getStream() {
            return stream;
        }

        /**
         * Wraps and builds the instance of the page model.
         */
        final static class Builder {
            private int page;
            private int total;
            private Stream<String> stream;

            /**
             * Constructs a page model with a page number.
             *
             * @param page a page number.
             * @return a builder of the page model.
             */
            public Builder page(final int page) {
                this.page = page;
                return this;
            }

            /**
             * Constructs a page model with a total.
             *
             * @param total a total of instances.
             * @return a builder of the page model.
             */
            public Builder total(final int total) {
                this.total = total;
                return this;
            }

            /**
             * Constructs a page model with a stream of names.
             *
             * @param stream a stream of names.
             * @return a builder of the page model.
             */
            public Builder stream(final Stream<String> stream) {
                this.stream = stream;
                return this;
            }

            /**
             * Builds a page model with required parameters.
             *
             * @return a builder of the page model.
             */
            public Page build() {
                return new Page(this);
            }
        }
    }
}
