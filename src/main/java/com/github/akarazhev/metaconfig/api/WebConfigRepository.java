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
 * {@inheritDoc}
 */
final class WebConfigRepository implements ConfigRepository {
    private final Config config;

    private WebConfigRepository(final Builder builder) {
        this.config = builder.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(Stream<String> stream) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> saveAndFlush(Stream<Config> stream) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Stream<String> stream) {
        return 0;
    }

    /**
     * Wraps and builds the instance of the web config repository.
     */
    public final static class Builder {
        private final Config config;

        /**
         * Constructs a web config repository with a required parameter.
         *
         * @param config the datasource.
         */
        Builder(final Config config) {
            this.config = config;
        }

        /**
         * Builds a web config repository with a required parameter.
         *
         * @return a builder of the web config repository.
         */
        public ConfigRepository build() {
            return new WebConfigRepository(this);
        }
    }
}
