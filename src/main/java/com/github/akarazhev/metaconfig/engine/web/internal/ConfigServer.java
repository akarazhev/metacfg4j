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
    private HttpServer server;

    public ConfigServer(final ConfigService configService) {
        int serverPort = 8000;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
            final ConfigController controller = new ConfigController(configService);
            server.createContext(ConfigController.API_STATUS, controller::status);
            server.setExecutor(null);
        } catch (final IOException e) {
            e.printStackTrace();
            server = null;
        }
    }

    public ConfigServer(final Config config, final ConfigService configService) {
        throw new RuntimeException("constructor with the configuration is not implemented");
    }

    @Override
    public WebServer start() throws IOException {
        if (server == null) {
            throw new IOException("Server was not created");
        }

        server.start();
        logger.log(Level.INFO, "Server started");
        return this;
    }

    @Override
    public void stop() throws IOException {
        if (server == null) {
            throw new IOException("Server was not created");
        }

        server.stop(0);
        logger.log(Level.INFO, "Server stopped");
    }
}
