package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.engine.web.internal.InternalServer;

public final class WebServers {

    private WebServers() {
        // Factory class
    }

    public static WebServer newServer() {
        return new InternalServer();
    }

    public static WebServer newServer(final Config config) {
        return new InternalServer(config);
    }
}
