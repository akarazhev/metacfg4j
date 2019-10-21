package com.github.akarazhev.metaconfig.api;

import java.util.Collection;
import java.util.function.Consumer;

public interface ConfigService {

    Config update(final Config config, final boolean override);

    Collection<String> getNames();

    Collection<Config> get();

    void remove(final String name);

    void accept(final Config config);

    void addConsumer(final Consumer<Config> consumer);
}
