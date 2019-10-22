package com.github.akarazhev.metaconfig.api;

import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Stream;

public interface Configurable extends Jsonable {

    Map<String, String> getAttributes();

    Stream<Property> getProperties();

    @Override
    default String toJson() {
        final StringWriter writable = new StringWriter();
        try {
            toJson(writable);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return writable.toString();
    }
}
