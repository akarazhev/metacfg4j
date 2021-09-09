package com.github.akarazhev.metaconfig.api.sql;

public final class PostgreSQL {

    public final static class CREATE_TABLE {

        public static final String CONFIGS =
                "CREATE TABLE IF NOT EXISTS %s " +
                        "(ID SERIAL NOT NULL PRIMARY KEY, " +
                        "NAME VARCHAR(255) NOT NULL, " +
                        "DESCRIPTION VARCHAR(1024), " +
                        "VERSION INT NOT NULL, " +
                        "UPDATED BIGINT NOT NULL);";
        public static final String CONFIG_ATTRIBUTES =
                "CREATE TABLE IF NOT EXISTS %2$s " +
                        "(ID SERIAL NOT NULL PRIMARY KEY, " +
                        "CONFIG_ID BIGINT NOT NULL, " +
                        "KEY VARCHAR(255) NOT NULL, " +
                        "VALUE VARCHAR(1024), " +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES %1$s(ID) ON DELETE CASCADE)";
        public static final String PROPERTIES =
                "CREATE TABLE IF NOT EXISTS %1$s (ID SERIAL NOT NULL PRIMARY KEY, " +
                        "PROPERTY_ID BIGINT, " +
                        "CONFIG_ID BIGINT NOT NULL, " +
                        "NAME VARCHAR(255) NOT NULL, " +
                        "CAPTION VARCHAR(255), " +
                        "DESCRIPTION VARCHAR(1024), " +
                        "TYPE VARCHAR(255) NOT NULL, " +
                        "VALUE VARCHAR(4096) NOT NULL, " +
                        "UPDATED BIGINT NOT NULL, " +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES %2$s(ID) ON DELETE CASCADE, " +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES %1$s(ID) ON DELETE CASCADE); ";
        public static final String PROPERTY_ATTRIBUTES =
                "CREATE TABLE IF NOT EXISTS %1$s " +
                        "(ID SERIAL NOT NULL PRIMARY KEY, " +
                        "PROPERTY_ID BIGINT NOT NULL, " +
                        "KEY VARCHAR(255) NOT NULL, " +
                        "VALUE VARCHAR(1024), " +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES %2$s(ID) ON DELETE CASCADE)";
    }
}
