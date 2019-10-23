package com.github.akarazhev.metaconfig.api;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface ConfigService {

    Config update(final Config config, final boolean override);

    Stream<String> getNames();

    Stream<Config> get();

    Optional<Config> get(final String name);

    void remove(final String name);

    void accept(final String name);

    void addConsumer(final Consumer<Config> consumer);
}
