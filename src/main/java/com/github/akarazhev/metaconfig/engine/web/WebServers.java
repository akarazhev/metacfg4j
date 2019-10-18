package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.engine.web.internal.ConfigServer;

public final class WebServers {

    private WebServers() {
        // Factory class
    }

    public static WebServer newServer(final ConfigService configService) {
        return new ConfigServer(configService);
    }

    public static WebServer newServer(final Config config, final ConfigService configService) {
        return new ConfigServer(config, configService);
    }
}
