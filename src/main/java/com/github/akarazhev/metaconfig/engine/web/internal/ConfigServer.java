package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigServer implements WebServer {
    private final static Logger logger = Logger.getLogger(ConfigServer.class.getSimpleName());
    private static HttpServer server = null;

    public ConfigServer(final ConfigService configService) throws IOException {
        if (server == null) {
            final ConfigController controller = new ConfigController(configService);
            server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext(ConfigController.API_STATUS, controller::status);
            server.setExecutor(null);
        } else {
            throw new RuntimeException("Server has been already created");
        }
    }

    public ConfigServer(final Config config, final ConfigService configService) throws IOException {
        throw new RuntimeException("constructor with the configuration is not implemented");
    }

    @Override
    public WebServer start() {
        server.start();
        logger.log(Level.INFO, "Server started");
        return this;
    }

    @Override
    public void stop() {
        server.stop(0);
        logger.log(Level.INFO, "Server stopped");
    }
}
