package com.github.akarazhev.metaconfig.api;

import javax.sql.DataSource;
import java.io.Closeable;

interface ConnectionPool extends Closeable {

    DataSource getDataSource();
}
