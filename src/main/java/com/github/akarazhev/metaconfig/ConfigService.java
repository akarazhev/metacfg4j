package com.github.akarazhev.metaconfig;

import java.util.Collection;

public interface ConfigService {

    Config update(final Config config, final boolean override);

    Collection<String> getNames();

    Collection<Config> get();

    void remove(final String name);
}
