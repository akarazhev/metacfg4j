/* Copyright 2019 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.extension.ExtJsonable;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Extends the basic interface of <code>ExtJsonable<code/>
 * and provides functionality for getting attributes and properties.
 *
 * @see ExtJsonable for more information.
 */
interface Configurable extends ExtJsonable {
    /**
     * Returns attributes which belong to configurations.
     *
     * @return attributes as a map.
     */
    Optional<Map<String, String>> getAttributes();

    /**
     * Returns attribute keys which belong to configurations.
     *
     * @return attribute keys as a stream.
     */
    Stream<String> getAttributeKeys();

    /**
     * Returns an attribute value by the key.
     *
     * @param key attribute key.
     * @return a value by the key.
     */
    Optional<String> getAttribute(final String key);

    /**
     * Returns properties which belong to configurations.
     *
     * @return properties as a stream.
     */
    Stream<Property> getProperties();

    /**
     * Returns a property by the name.
     *
     * @param name a property name.
     * @return a property.
     */
    Optional<Property> getProperty(final String name);

    /**
     * Provides methods for getting attributes and properties from json objects.
     */
    class ConfigBuilder {
        /**
         * Returns attributes which belong to configurations.
         *
         * @param object a json object with attributes.
         * @return attributes as a map.
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
         * Returns properties which belong to configurations.
         *
         * @param object a json object with properties.
         * @return properties as a stream.
         */
        Stream<Property> getProperties(final Object object) {
            return object != null ?
                    ((JsonArray) object).stream().
                            map(jsonObject -> new Property.Builder((JsonObject) jsonObject).build()) :
                    Stream.empty();
        }
    }
}
