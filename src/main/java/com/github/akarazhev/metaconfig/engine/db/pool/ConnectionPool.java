package com.github.akarazhev.metaconfig.engine.db.pool;

import javax.sql.DataSource;
import java.io.Closeable;

public interface ConnectionPool extends Closeable {

    DataSource getDataSource();
}
