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
     * @param names a stream of names.
     * @return a stream of configurations models.
     */
    Stream<Config> findByNames(final Stream<String> names);

    /**
     * Returns all configuration names.
     *
     * @return a stream of configuration names.
     */
    Stream<String> findNames();

    /**
     * Saves and flushes configuration models.
     *
     * @param configs a stream of configuration models.
     * @return a stream of updated configuration models.
     */
    Stream<Config> saveAndFlush(final Stream<Config> configs);

    /**
     * Deletes configuration models.
     *
     * @param names a stream of names.
     * @return a number of deleted models.
     */
    int delete(final Stream<String> names);
}
