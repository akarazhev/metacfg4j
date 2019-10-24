package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.json_simple.ExtJsonable;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

interface Configurable extends ExtJsonable {

    Map<String, String> getAttributes();

    Stream<Property> getProperties();

    class ConfigBuilder {

        Optional<Map<String, String>> getAttributes(final Object object) {
            // todo re-implement it
            if (object != null) {
                final JsonObject jsonObject = (JsonObject) object;
                final Map<String, String> attributes = new HashMap<>();
                for (Object key : jsonObject.keySet()) {
                    attributes.put((String) key, (String) jsonObject.get(key));
                }

                return Optional.of(attributes);
            }

            return Optional.empty();
        }

        Stream<Property> getProperties(final Object object) {
            return object != null ?
                    ((JsonArray) object).stream().
                            map(jsonObject -> new Property.Builder((JsonObject) jsonObject).build()) :
                    Stream.empty();
        }
    }
}
