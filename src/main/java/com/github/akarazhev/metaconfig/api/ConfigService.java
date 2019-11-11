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

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Provides service methods to create, read, update and delete operations.
 */
public interface ConfigService {
    /**
     * Updates configuration models.
     *
     * @param configs  a stream of configuration models.
     * @param override indicates if the existed configuration model should be override or not.
     * @return a stream of updated configuration models.
     */
    Stream<Config> update(final Stream<Config> configs, final boolean override);

    /**
     * Returns all configuration names.
     *
     * @return a stream of configuration names.
     */
    Stream<String> getNames();

    /**
     * Returns all configuration models.
     *
     * @return a stream of configurations models.
     */
    Stream<Config> get();

    /**
     * Returns configuration models by names.
     *
     * @param names a stream of names.
     * @return a stream of configurations models.
     */
    Stream<Config> get(final Stream<String> names);

    /**
     * Removes configuration models by names.
     *
     * @param names a stream of names.
     * @return a number of deleted models.
     */
    int remove(final Stream<String> names);

    /**
     * Removes a configuration model by the name.
     *
     * @param name a configuration name.
     */
    void accept(final String name);

    /**
     * Adds a consumer to provide an action.
     *
     * @param consumer an implementation of the consumer.
     */
    void addConsumer(final Consumer<Config> consumer);
}
