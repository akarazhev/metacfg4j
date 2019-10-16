package com.github.akarazhev.metaconfig.web;

import com.github.akarazhev.metaconfig.Config;
import com.github.akarazhev.metaconfig.web.internal.InternalServer;

public final class WebServers {

    private WebServers() {
    }

    public static WebServer newDefaultWebServer() {
        return new InternalServer();
    }

    public static WebServer newWebServer(final Config config) {
        return new InternalServer(config);
    }
}
