package com.github.akarazhev.metaconfig.api;

import javax.sql.DataSource;
import java.util.stream.Stream;

final class DdConfigRepository implements ConfigRepository {

    private final DataSource dataSource;

    public DdConfigRepository(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Stream<Config> findByName(final String name) {
        throw new RuntimeException("findByName is not implemented");
    }

    @Override
    public Stream<String> findNames() {
        throw new RuntimeException("findNames is not implemented");
    }

    @Override
    public Config saveAndFlush(final Config config) {
        throw new RuntimeException("saveAndFlush is not implemented");
    }

    @Override
    public void delete(final String name) {
        throw new RuntimeException("delete is not implemented");
    }
}
