package com.github.akarazhev.metaconfig.store;

import java.sql.SQLException;

public interface DbServer {

    void start() throws SQLException;

    void stop();
}
