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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@inheritDoc}
 */
final class ConfigServiceImpl implements ConfigService {
    private final ConfigRepository configRepository;
    private Consumer<Config> consumer;

    private ConfigServiceImpl(final Builder builder) {
        this.configRepository = builder.configRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config update(final Config config, final boolean override) {
        // Ignore override at this moment
        return configRepository.saveAndFlush(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> getNames() {
        return configRepository.findNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> get() {
        Stream<String> names = configRepository.findNames();
        Collection<Config> configs = new ArrayList<>((int) names.count());
        names.forEach(name -> configs.addAll(configRepository.findByName(name).collect(Collectors.toList())));
        return configs.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Config> get(final String name) {
        return configRepository.findByName(name).findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final String name) {
        get(name).ifPresent(config -> configRepository.delete(config.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(final String name) {
        if (consumer != null) {
            get(name).ifPresent(config -> consumer.accept(config));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addConsumer(final Consumer<Config> consumer) {
        this.consumer = consumer;
    }

    /**
     * Wraps and builds the instance of the config service.
     */
    public final static class Builder {
        private final ConfigRepository configRepository;

        /**
         * Constructs a config service with a required parameter.
         *
         * @param configRepository a config repository.
         */
        public Builder(final ConfigRepository configRepository) {
            this.configRepository = configRepository;
        }

        /**
         * Builds a config service with a required parameter.
         *
         * @return a builder of the config service.
         */
        public ConfigService build() {
            return new ConfigServiceImpl(this);
        }
    }
}
