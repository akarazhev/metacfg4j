package com.github.akarazhev.metaconfig.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MetaConfigService implements ConfigService {
    private final ConfigRepository configRepository;

    public MetaConfigService(final ConfigRepository configRepository) {
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
}
