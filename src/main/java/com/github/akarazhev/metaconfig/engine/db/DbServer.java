package com.github.akarazhev.metaconfig.engine.db;

import java.sql.SQLException;

public interface DbServer {

    void start() throws SQLException;

    void stop();
}
