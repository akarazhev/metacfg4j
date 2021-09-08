package com.github.akarazhev.metaconfig.api.sql;

public final class SQL {

    public final static class INSERT {

        public static final String CONFIGS =
                "INSERT INTO %s (NAME, DESCRIPTION, VERSION, UPDATED) VALUES (?, ?, ?, ?);";
        public static final String CONFIG_ATTRIBUTES =
                "INSERT INTO %s (CONFIG_ID, KEY, VALUE) VALUES (?, ?, ?);";
        public static final String PROPERTIES =
                "INSERT INTO %s (CONFIG_ID, NAME, CAPTION, DESCRIPTION, TYPE, VALUE, UPDATED) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);";
        public static final String PROPERTY_ATTRIBUTES =
                "INSERT INTO %s (PROPERTY_ID, KEY, VALUE) VALUES (?, ?, ?);";
        public static final String SUB_PROPERTIES =
                "INSERT INTO %s (PROPERTY_ID, CONFIG_ID, NAME, CAPTION, DESCRIPTION, TYPE, VALUE, " +
                        "UPDATED) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    }

    public final static class UPDATE {

        public static final String CONFIGS =
                "UPDATE %s SET NAME = ?, DESCRIPTION = ?, VERSION = ?, UPDATED = ? " +
                        "WHERE ID = ? AND VERSION = ?;";
        public static final String CONFIG_ATTRIBUTE =
                "UPDATE %s SET VALUE = ? WHERE CONFIG_ID = ? AND KEY = ?;";
        public static final String PROPERTIES =
                "UPDATE %s SET NAME = ?, CAPTION = ?, DESCRIPTION = ?, TYPE = ?, VALUE = ?, " +
                        "UPDATED = ? WHERE ID = ?;";
        public static final String PROPERTY_ATTRIBUTE =
                "UPDATE %s SET VALUE = ? WHERE PROPERTY_ID = ? AND KEY = ?;";
    }

    public final static class DELETE {

        public static final String CONFIGS =
                "DELETE FROM %1$s WHERE %1$s.NAME = ?";
        public static final String CONFIG_ATTRIBUTES =
                "DELETE FROM %1$s WHERE %1$s.CONFIG_ID = ?;";
        public static final String PROPERTY_ATTRIBUTES =
                "DELETE FROM %1$s WHERE %1$s.PROPERTY_ID = ?;";
        public static final String CONFIG_ATTRIBUTE =
                "DELETE FROM %1$s WHERE %1$s.CONFIG_ID = ? AND %1$s.KEY = ? AND %1$s.VALUE = ?;";
        public static final String PROPERTY_ATTRIBUTE =
                "DELETE FROM %1$s WHERE %1$s.PROPERTY_ID = ? AND %1$s.KEY = ? AND %1$s.VALUE = ?;";
        public static final String PROPERTIES =
                "DELETE FROM %1$s WHERE %1$s.CONFIG_ID = ?;";
        public static final String PROPERTY =
                "DELETE FROM %1$s WHERE %1$s.ID = ?;";
    }

    public final static class SELECT {

        public static final String CONFIG_NAMES =
                "SELECT C.NAME FROM %s AS C ORDER BY C.NAME;";
        public static final String COUNT_CONFIG_NAMES_BY_NAME =
                "SELECT COUNT(DISTINCT C.NAME) FROM %1$s AS C " +
                        "INNER JOIN %2$s AS CA ON C.ID = CA.CONFIG_ID " +
                        "WHERE (C.NAME LIKE ?)";
        public static final String CONFIG_NAMES_BY_NAME =
                "SELECT DISTINCT C.NAME FROM %1$s AS C " +
                        "INNER JOIN %2$s AS CA ON C.ID = CA.CONFIG_ID " +
                        "WHERE (C.NAME LIKE ?)";
        public static final String CONFIG_ATTRIBUTES =
                "SELECT CA.KEY, CA.VALUE FROM %s AS CA WHERE CA.CONFIG_ID = ?;";
        public static final String PROPERTY_ATTRIBUTES =
                "SELECT PA.KEY, PA.VALUE FROM %s AS PA WHERE PA.PROPERTY_ID = ?;";
        public static final String PROPERTY_ID_UPDATED =
                "SELECT P.ID, P.UPDATED FROM %s AS P WHERE P.CONFIG_ID = ?;";
        public static final String CONFIG_VERSION_UPDATED =
                "SELECT C.ID, C.VERSION, C.UPDATED FROM %s AS C WHERE ";
        public static final String CONFIGS =
                "SELECT C.ID, C.NAME, C.DESCRIPTION, C.VERSION, C.UPDATED, CA.KEY, " +
                        "CA.VALUE, P.ID, P.PROPERTY_ID, P.NAME , P.CAPTION, " +
                        "P.DESCRIPTION, P.TYPE, P.VALUE, P.UPDATED, PA.KEY, PA.VALUE " +
                        "FROM %1$s AS C " +
                        "LEFT JOIN %3$s AS P ON C.ID = P.CONFIG_ID " +
                        "LEFT JOIN %2$s AS CA ON C.ID = CA.CONFIG_ID " +
                        "LEFT JOIN %4$s AS PA ON P.ID = PA.PROPERTY_ID " +
                        "WHERE C.NAME = ?";
    }

    public final static class CREATE_TABLE {

        public static final String CONFIGS =
                "CREATE TABLE IF NOT EXISTS %s " +
                        "(ID BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                        "NAME VARCHAR(255) NOT NULL, " +
                        "DESCRIPTION VARCHAR(1024), " +
                        "VERSION INT NOT NULL, " +
                        "UPDATED BIGINT NOT NULL);";
        public static final String CONFIG_ATTRIBUTES =
                "CREATE TABLE IF NOT EXISTS %2$s " +
                        "(ID BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                        "CONFIG_ID BIGINT NOT NULL, " +
                        "KEY VARCHAR(255) NOT NULL, " +
                        "VALUE VARCHAR(1024), " +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES %1$s(ID) ON DELETE CASCADE)";
        public static final String PROPERTIES =
                "CREATE TABLE IF NOT EXISTS %1$s " +
                        "(ID BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                        "PROPERTY_ID BIGINT, " +
                        "CONFIG_ID BIGINT NOT NULL, " +
                        "NAME VARCHAR(255) NOT NULL, " +
                        "CAPTION VARCHAR(255), " +
                        "DESCRIPTION VARCHAR(1024), " +
                        "TYPE ENUM ('BOOL', 'DOUBLE', 'LONG', 'STRING', 'STRING_ARRAY') NOT NULL, " +
                        "VALUE VARCHAR(4096) NOT NULL, " +
                        "UPDATED BIGINT NOT NULL," +
                        "FOREIGN KEY(CONFIG_ID) REFERENCES %2$s(ID) ON DELETE CASCADE," +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES %1$s(ID) ON DELETE CASCADE)";
        public static final String PROPERTY_ATTRIBUTES =
                "CREATE TABLE IF NOT EXISTS %1$s " +
                        "(ID BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
                        "PROPERTY_ID BIGINT NOT NULL, " +
                        "KEY VARCHAR(255) NOT NULL, " +
                        "VALUE VARCHAR(1024), " +
                        "FOREIGN KEY(PROPERTY_ID) REFERENCES %2$s(ID) ON DELETE CASCADE)";
    }
}
