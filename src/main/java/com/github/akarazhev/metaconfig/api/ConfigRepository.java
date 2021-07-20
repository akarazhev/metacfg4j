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
     * Returns selected configuration names by a configuration page request.
     *
     * @param request a configuration page request that has parameters: name, page, size, ascending.
     * @return a page response with configuration names.
     */
    PageResponse findByPageRequest(final PageRequest request);

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
}
