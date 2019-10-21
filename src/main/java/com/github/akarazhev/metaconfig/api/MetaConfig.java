package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.engine.db.DbServer;
import com.github.akarazhev.metaconfig.engine.db.DbServers;
import com.github.akarazhev.metaconfig.engine.db.pool.ConnectionPool;
import com.github.akarazhev.metaconfig.engine.db.pool.ConnectionPools;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.engine.web.WebServers;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

public final class MetaConfig implements ConfigService, Closeable {
    private final DbServer dbServer;
    private final WebServer webServer;
    private final ConnectionPool connectionPool;
    private final ConfigService configService;

    private MetaConfig(final DbServer dbServer, final WebServer webServer, final ConnectionPool connectionPool,
                       final ConfigService configService) {
        this.dbServer = dbServer;
        this.webServer = webServer;
        this.connectionPool = connectionPool;
        this.configService = configService;
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
    public void accept(final Config config) {
        configService.accept(config);
    }

    @Override
    public void addConsumer(final Consumer<Config> consumer) {
        configService.addConsumer(consumer);
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

    public final static class Builder {
        private Config dbConfig;
        private Config webConfig;
        private Config poolConfig;

        public Builder() {
        }

        public Builder dbServer(final Config config) {
            this.dbConfig = config;
            return this;
        }

        public Builder webServer(final Config config) {
            this.webConfig = config;
            return this;
        }

        public Builder connectionPool(final Config config) {
            this.poolConfig = config;
            return this;
        }

        public MetaConfig build() {
            DbServer dbServer;
            ConnectionPool connectionPool;
            WebServer webServer;
            ConfigService configService;

            try {
                // DB Server
                if (dbConfig != null) {
                    dbServer = DbServers.newServer(dbConfig).start();
                } else {
                    dbServer = DbServers.newServer().start();
                }
                // Connection pool
                if (poolConfig != null) {
                    connectionPool = ConnectionPools.newPool(poolConfig);
                } else {
                    connectionPool = ConnectionPools.newPool();
                }
                // Config service
                configService = new ConfigServiceImpl(new ConfigRepositoryImpl(connectionPool.getDataSource()));
                // Web server
                if (webConfig != null) {
                    webServer = WebServers.newServer(webConfig, configService).start();
                } else {
                    webServer = WebServers.newServer(configService).start();
                }
                // Create the main instance
                return new MetaConfig(dbServer, webServer, connectionPool, configService);
            } catch (Exception e) {
                throw new RuntimeException("The MetaConfig instance can't be created");
            }
        }
    }
}
