/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.api;

import java.util.function.Consumer;
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
    public Stream<Config> update(final Stream<Config> stream) {
        return configRepository.saveAndFlush(stream);
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
    public PageResponse getNames(final PageRequest request) {
        return configRepository.findByPageRequest(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> get() {
        return get(getNames());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> get(final Stream<String> stream) {
        return configRepository.findByNames(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remove(final Stream<String> stream) {
        return configRepository.delete(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(final Stream<String> stream) {
        if (configRepository instanceof WebConfigRepository) {
            ((WebConfigRepository) configRepository).accept(stream);
        } else {
            if (consumer != null) {
                get(stream).forEach(config -> consumer.accept(config));
            }
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
    final static class Builder {
        private final ConfigRepository configRepository;

        /**
         * Constructs a config service with a required parameter.
         *
         * @param configRepository a config repository.
         */
        Builder(final ConfigRepository configRepository) {
            this.configRepository = configRepository;
        }

        /**
         * Builds a config service with a required parameter.
         *
         * @return a builder of the config service.
         */
        ConfigService build() {
            return new ConfigServiceImpl(this);
        }
    }
}
