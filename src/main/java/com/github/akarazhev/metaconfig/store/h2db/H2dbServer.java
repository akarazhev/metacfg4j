package com.github.akarazhev.metaconfig.store.h2db;

import com.github.akarazhev.metaconfig.Config;
import com.github.akarazhev.metaconfig.store.DbServer;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class H2dbServer implements DbServer {
    private final static Logger logger = Logger.getLogger("H2dbServer");
    private final Server server;

    public H2dbServer() throws SQLException {
        server = Server.createTcpServer("-tcp", "-tcpPort", "8043");
    }

    public H2dbServer(final Config config) throws SQLException {
        // todo
        server = null;
    }

    @Override
    public void start() throws SQLException {
        server.start();
        logger.log(Level.INFO, "Server started");
    }

    @Override
    public void stop() {
        server.stop();
        logger.log(Level.INFO, "Server stopped");
    }
}
