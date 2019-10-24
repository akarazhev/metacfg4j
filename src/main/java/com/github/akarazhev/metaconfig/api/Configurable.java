package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.json_simple.ExtJsonable;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 *
 */
interface Configurable extends ExtJsonable {
    /**
     *
     * @return
     */
    Map<String, String> getAttributes();

    /**
     *
     * @return
     */
    Stream<Property> getProperties();

    /**
     *
     */
    class ConfigBuilder {

        /**
         *
         * @param object
         * @return
         */
        Optional<Map<String, String>> getAttributes(final Object object) {
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

        /**
         *
         * @param object
         * @return
         */
        Stream<Property> getProperties(final Object object) {
            return object != null ?
                    ((JsonArray) object).stream().map(jsonObject -> new Property.Builder((JsonObject) jsonObject).build()) :
                    Stream.empty();
        }
    }
}
