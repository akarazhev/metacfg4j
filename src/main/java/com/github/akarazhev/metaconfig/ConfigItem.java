package com.github.akarazhev.metaconfig;

import java.util.Collection;
import java.util.Map;

public final class ConfigItem {

    public static enum Type {
        BOOLEAN,
        DOUBLE,
        LONG,
        STRING,
        STRING_ARRAY
    }

    private String name;
    private String caption;
    private String description;
    private String defaultValue;
    private String value;
    private Type type = Type.STRING;
    private Map<String, String> attributes;
    private Collection<ConfigItem> children;
}
