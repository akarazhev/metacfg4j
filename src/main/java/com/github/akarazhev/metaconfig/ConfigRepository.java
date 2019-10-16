package com.github.akarazhev.metaconfig;

import java.util.stream.Stream;

interface ConfigRepository {

    Stream<Config> findByName(final String name);

    Stream<String> findNames();

    Config saveAndFlush(final Config config);

    void delete(final String name);
}
