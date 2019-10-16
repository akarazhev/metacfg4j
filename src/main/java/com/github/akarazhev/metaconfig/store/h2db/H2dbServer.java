package com.github.akarazhev.metaconfig.store.h2db;

import com.github.akarazhev.metaconfig.MetaConfig;
import com.github.akarazhev.metaconfig.store.DbServer;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class H2dbServer implements DbServer {
    private final static Logger logger = Logger.getLogger("H2dbServer");
    private Server server;

    public H2dbServer() throws SQLException {
        server = Server.createTcpServer("-tcp", "-tcpPort", "8043");
    }

    public H2dbServer(MetaConfig metaConfig) throws SQLException {
        // todo
    }

    @Override
    public void start() throws SQLException {
        server.start();
        logger.log(Level.INFO, "H2dbServer started");
    }

    @Override
    public void stop() {
        server.stop();
        logger.log(Level.INFO, "H2dbServer stopped");
    }
}
