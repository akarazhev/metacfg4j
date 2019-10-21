package com.github.akarazhev.metaconfig.api;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

final class ConfigRepositoryImpl implements ConfigRepository {

    private final DataSource dataSource;
    private final Map<String, Config> inMemDataSource;

    ConfigRepositoryImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
        this.inMemDataSource = new ConcurrentHashMap<>();
    }

    @Override
    public Stream<Config> findByName(final String name) {
        return Stream.of(inMemDataSource.get(name));
    }

    @Override
    public Stream<String> findNames() {
        return inMemDataSource.keySet().stream();
    }

    @Override
    public Config saveAndFlush(final Config config) {
        return inMemDataSource.put(config.getName(), config);
    }

    @Override
    public void delete(final String name) {
        inMemDataSource.remove(name);
    }
}
