package com.github.akarazhev.metaconfig.web;

import com.github.akarazhev.metaconfig.MetaConfig;
import com.github.akarazhev.metaconfig.web.jetty.JettyServer;

public final class WebServers {

    private WebServers() {
    }

    public static WebServer newDefaultWebServer() {
        return new JettyServer();
    }

    public static WebServer newWebServer(final MetaConfig metaConfig) {
        return new JettyServer(metaConfig);
    }
}
