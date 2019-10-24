package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.json_simple.ExtJsonable;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
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

        Optional<Collection<Property>> getProperties(final Object object) {
            // todo re-implement it
            if (object != null) {
                final JsonArray jsonArray = (JsonArray) object;
                final Collection<Property> properties = new ArrayList<>(jsonArray.size());
                for (Object jsonProperty : jsonArray) {
                    properties.add(new Property.Builder((JsonObject) jsonProperty).build());
                }

                return Optional.of(properties);
            }

            return Optional.empty();
        }
    }
}
