package com.github.akarazhev.metaconfig.api;

import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

public final class Property implements Jsonable {

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
    private Collection<Property> children;

    @Override
    public String toJson() {
        return null;
    }

    @Override
    public void toJson(Writer writer) throws IOException {

    }
}
