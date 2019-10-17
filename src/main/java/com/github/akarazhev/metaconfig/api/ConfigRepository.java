package com.github.akarazhev.metaconfig.api;

import java.util.stream.Stream;

interface ConfigRepository {

    Stream<Config> findByName(final String name);

    Stream<String> findNames();

    Config saveAndFlush(final Config config);

    void delete(final String name);
}
