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
interface ConfigRepository { // todo: all methods as a stream
    /**
     * Returns configuration models for a configuration name.
     *
     * @param name a configuration name.
     * @return a stream of configurations models.
     */
    Stream<Config> findByName(final String name);

    /**
     * Returns all configuration names.
     *
     * @return a stream of configuration names.
     */
    Stream<String> findNames();

    /**
     * Saves and flushes a configuration model.
     *
     * @param config a configuration model.
     * @return a stream of an updated configuration model.
     */
    Stream<Config> saveAndFlush(final Config config);

    /**
     * Deletes a configuration model.
     *
     * @param id a configuration id.
     */
    void delete(final int id);
}
