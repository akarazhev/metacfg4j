package com.github.akarazhev.metaconfig;

import com.github.akarazhev.metaconfig.api.Config;

import java.io.Closeable;
import java.io.IOException;

public final class MetaConfig implements Closeable {

    public MetaConfig() {
    }

    public MetaConfig(Config config) {
    }

    @Override
    public void close() throws IOException {
        throw new RuntimeException("close is not implemented");
    }
}
