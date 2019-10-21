package com.github.akarazhev.metaconfig.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ConfigServiceImpl implements ConfigService {
    private final ConfigRepository configRepository;
    private Consumer<Config> consumer;

    ConfigServiceImpl(final ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public Config update(final Config config, final boolean override) {
        // Ignore override at this moment
        return configRepository.saveAndFlush(config);
    }

    @Override
    public Collection<String> getNames() {
        try (Stream<String> stream = configRepository.findNames()) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public Collection<Config> get() {
        Collection<Config> configs = new LinkedList<>();
        configRepository.findNames().forEach(name ->
                configs.addAll(configRepository.findByName(name).collect(Collectors.toList())));
        return configs;
    }

    @Override
    public void remove(final String name) {
        configRepository.delete(name);
    }

    @Override
    public void accept(final Config config) {
        if (consumer != null) {
            consumer.accept(config);
        }
    }

    @Override
    public void addConsumer(final Consumer<Config> consumer) {
        this.consumer = consumer;
    }
}
