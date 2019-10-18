package com.github.akarazhev.metaconfig.engine.db;

import java.sql.SQLException;

public interface DbServer {

    DbServer start() throws SQLException;

    void stop();
}
