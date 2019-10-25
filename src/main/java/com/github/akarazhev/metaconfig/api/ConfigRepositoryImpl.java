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

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * {@inheritDoc}
 */
final class ConfigRepositoryImpl implements ConfigRepository {
    private final DataSource dataSource;
    private final Map<String, Config> inMemDataSource;

    ConfigRepositoryImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
        this.inMemDataSource = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByName(final String name) {
        return Stream.of(inMemDataSource.get(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        return inMemDataSource.keySet().stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config saveAndFlush(final Config config) {
        inMemDataSource.put(config.getName(), config);
        return inMemDataSource.get(config.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String name) {
        inMemDataSource.remove(name);
    }
}
