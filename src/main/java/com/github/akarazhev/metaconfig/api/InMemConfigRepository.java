package com.github.akarazhev.metaconfig.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

final class InMemConfigRepository implements ConfigRepository {

    private final Map<String, Config> inMemStore;

    public InMemConfigRepository() {
        this.inMemStore = new ConcurrentHashMap<>();
    }

    @Override
    public Stream<Config> findByName(final String name) {
        return Stream.of(inMemStore.get(name));
    }

    @Override
    public Stream<String> findNames() {
        return inMemStore.keySet().stream();
    }

    @Override
    public Config saveAndFlush(final Config config) {
        return inMemStore.put(config.getName(), config);
    }

    @Override
    public void delete(final String name) {
        inMemStore.remove(name);
    }
}
