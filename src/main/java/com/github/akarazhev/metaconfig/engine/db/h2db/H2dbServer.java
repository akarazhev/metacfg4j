package com.github.akarazhev.metaconfig.engine.db.h2db;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.engine.db.DbServer;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class H2dbServer implements DbServer {
    private final static Logger logger = Logger.getLogger(H2dbServer.class.getSimpleName());
    private static Server server = null;

    public H2dbServer() throws SQLException {
        if (server == null) {
            server = Server.createTcpServer("-tcp", "-tcpPort", "8043");
        } else {
            throw new RuntimeException("Server has been already created");
        }
    }

    public H2dbServer(final Config config) throws SQLException {
        throw new RuntimeException("constructor with the configuration is not implemented");
    }

    @Override
    public DbServer start() throws SQLException {
        if (!server.isRunning(true)) {
            server.start();
            logger.log(Level.INFO, "Server started");
        }

        return this;
    }

    @Override
    public void stop() {
        if (server.isRunning(true)) {
            server.stop();
            logger.log(Level.INFO, "Server stopped");
        }
    }
}
