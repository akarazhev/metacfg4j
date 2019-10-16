package com.github.akarazhev.metaconfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

final class ConfigRepositoryImpl implements ConfigRepository {

    private static final Map<String, Config> STORE = new ConcurrentHashMap<>();

    @Override
    public Stream<Config> findByName(final String name) {
        return Stream.of(STORE.get(name));
    }

    @Override
    public Stream<String> findNames() {
        return STORE.keySet().stream();
    }

    @Override
    public Config saveAndFlush(final Config config) {
        return STORE.put(config.getName(), config);
    }

    @Override
    public void delete(final String name) {
        STORE.remove(name);
    }
}
