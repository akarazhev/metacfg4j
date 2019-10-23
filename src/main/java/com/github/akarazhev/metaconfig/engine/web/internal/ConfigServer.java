package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_NAMES;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_SECTION;

public final class ConfigServer implements WebServer {
    private final static Logger logger = Logger.getLogger(ConfigServer.class.getSimpleName());
    private static HttpServer server = null;

    public ConfigServer(final ConfigService configService) throws IOException {
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(8000), 0);
            AbstractController controller = new ConfigNamesController(configService);
            server.createContext(CONFIG_NAMES, controller::handle);
            controller = new ConfigSectionController(configService);
            server.createContext(CONFIG_SECTION, controller::handle);
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
