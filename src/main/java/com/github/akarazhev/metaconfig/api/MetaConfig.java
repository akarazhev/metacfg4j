package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.engine.db.DbServer;
import com.github.akarazhev.metaconfig.engine.db.DbServers;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.engine.web.WebServers;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

public final class MetaConfig implements ConfigService, Closeable {
    private DbServer dbServer;
    private WebServer webServer;
    private ConnectionPool connectionPool;
    private ConfigService configService;

    public MetaConfig() {
        try {
            // DB Server
            dbServer = DbServers.newServer().start();
            // Connection pool
            connectionPool = ConnectionPools.newConnectionPool();
        } catch (Exception e) {
            throw new RuntimeException("The H2DB instance can't be started");
        }

        // Config service
        configService = new ConfigServiceImpl(new ConfigRepositoryImpl(connectionPool.getDataSource()));
        // Web server
        try {
            webServer = WebServers.newServer(configService).start();
        } catch (IOException e) {
            throw new RuntimeException("The config server instance can't be started");
        }
    }

    public MetaConfig(Config config) {
        throw new RuntimeException("constructor with the configuration is not implemented");
    }

    @Override
    public Config update(final Config config, final boolean override) {
        return configService.update(config, override);
    }

    @Override
    public Collection<String> getNames() {
        return configService.getNames();
    }

    @Override
    public Collection<Config> get() {
        return configService.get();
    }

    @Override
    public void remove(final String name) {
        configService.remove(name);
    }

    @Override
    public void close() throws IOException {
        // Stop the web server
        if (webServer != null) {
            webServer.stop();
        }
        // Close the connection pool
        if (connectionPool != null) {
            connectionPool.close();
        }
        // Stop the database server
        if (dbServer != null) {
            dbServer.stop();
        }
    }
}
