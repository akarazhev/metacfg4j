/* Copyright 2019-2021 Andrey Karazhev
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

import com.github.akarazhev.metaconfig.UnitTest;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Config test")
final class ConfigTest extends UnitTest {

    @Test
    @DisplayName("Create a config")
    void createConfig() {
        final var config = new Config.Builder(CONFIG, Collections.emptyList()).build();
        // Check test results
        assertEquals(CONFIG, config.getName());
        assertFalse(config.getDescription().isPresent());
        assertEquals(1, config.getVersion());
        assertTrue(config.getUpdated() > 0);
        assertTrue(config.getAttributes().isPresent());
        assertTrue(config.getAttributes().get().isEmpty());
        assertFalse(config.getAttribute("key").isPresent());
        assertEquals(0, config.getAttributeKeys().count());
    }

    @Test
    @DisplayName("Create a config exception")
    void createConfigException() {
        // Check test results
        assertThrows(IllegalArgumentException.class,
                () -> new Config.Builder(CONFIG, Collections.emptyList()).id(0));
        assertThrows(IllegalArgumentException.class,
                () -> new Config.Builder(CONFIG, Collections.emptyList()).version(0));
        assertThrows(IllegalArgumentException.class,
                () -> new Config.Builder(CONFIG, Collections.emptyList()).updated(0));
    }

    @Test
    @DisplayName("Create a config with properties")
    void createConfigWithParameters() {
        final var config = getConfig(Collections.singletonList(getProperty()));
        // Check test results
        assertEquals(100, config.getId());
        assertEquals(CONFIG, config.getName());
        assertTrue(config.getDescription().isPresent());
        assertEquals("Description", config.getDescription().get());
        assertEquals(2, config.getVersion());
        assertEquals(UPDATED, config.getUpdated());
        assertTrue(config.getAttributes().isPresent());
        assertEquals(2, config.getAttributes().get().size());
        assertEquals(2, config.getProperties().count());
        assertTrue(config.getProperty("Property-1").isPresent());
        assertTrue(config.getProperty("Property-2").isPresent());
    }

    @Test
    @DisplayName("Create a config with deleted properties")
    void createConfigWithDeletedProperties() {
        final var path = new String[]{"Property", "Sub-property-1", "Sub-property-2", "Sub-property-3"};
        final var property = new Property.Builder("Property", "Value").
                property(new String[]{"Sub-property-1", "Sub-property-2"},
                        new Property.Builder("Sub-property-3", "Sub-value-3").build()).build();
        final var config = new Config.Builder(CONFIG, Collections.singletonList(property)).build();
        // Check test results
        assertTrue(config.getProperty(path).isPresent());

        final var updatedConfig = new Config.Builder(config).deleteProperty(path).build();
        // Check test results
        assertFalse(updatedConfig.getProperty(path).isPresent());
    }

    @Test
    @DisplayName("Create a config with updated properties")
    void createConfigWithUpdatedProperties() {
        final var path = new String[]{"Property", "Property-2"};
        final var property = new Property.Builder("Property", "Value").
                property(new String[]{"Property-1", "Property-1.2"},
                        new Property.Builder("Property-1.3", "Value-1.3").build()).
                property(new String[]{"Property-2", "Property-2.2"},
                        new Property.Builder("Property-2.3", "Value-2.3").build()).
                property(new String[]{"Property-3", "Property-3.2"},
                        new Property.Builder("Property-3.3", "Value-3.3").build()).build();
        final var config = new Config.Builder(CONFIG, Collections.singletonList(property)).build();
        // Check test results
        assertTrue(config.getProperty(path).isPresent());

        final var updatedConfig = new Config.Builder(config).deleteProperty(path).build();
        // Check test results
        assertFalse(updatedConfig.getProperty(path).isPresent());
    }

    @Test
    @DisplayName("Compare a wrong config")
    void compareWrongConfig() {
        // Check test results
        assertNotEquals(getConfig(Collections.emptyList()), getProperty());
    }

    @Test
    @DisplayName("Compare a null config")
    void compareNullConfig() {
        // Check test results
        assertNotEquals(getConfig(Collections.emptyList()), null);
    }

    @Test
    @DisplayName("Compare a config")
    void compareConfig() {
        final var firstConfig = getConfig(Collections.emptyList());
        // Check test results
        assertEquals(firstConfig, firstConfig);
    }

    @Test
    @DisplayName("Compare two simple configs")
    void compareTwoSimpleConfigs() {
        final var firstConfig = getConfig(Collections.emptyList());
        final var secondConfig = getConfig(Collections.emptyList());
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Compare two configs")
    void compareTwoConfigs() {
        final var firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final var firstConfig = new Config.Builder(CONFIG, Collections.singletonList(firstProperty)).
                id(1).
                description("Description").
                version(1).
                updated(UPDATED).
                attribute("key_1", "value_1").
                attributes(Collections.singletonMap("key_2", "value_2")).
                property(new String[0], new Property.Builder("Property-2", "Value-2").build()).
                build();
        final var secondProperty = new Property.Builder("Property-1", "Value-1").build();
        final var secondConfig = new Config.Builder(CONFIG, Collections.singletonList(secondProperty)).
                id(1).
                description("Description").
                version(1).
                updated(UPDATED).
                attribute("key_1", "value_1").
                attributes(Collections.singletonMap("key_2", "value_2")).
                property(new String[0], new Property.Builder("Property-2", "Value-2").build()).
                build();
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Check hash codes of two configs")
    void checkHashCodesOfTwoConfigs() {
        final var firstConfig = new Config.Builder(CONFIG, Collections.emptyList()).build();
        final var secondConfig = new Config.Builder(CONFIG, Collections.emptyList()).build();
        // Check test results
        assertEquals(firstConfig.hashCode(), secondConfig.hashCode());
    }

    @Test
    @DisplayName("Check toString() of two configs")
    void checkToStringOfTwoConfigs() {
        final var firstConfig = new Config.Builder(CONFIG, Collections.emptyList()).build();
        final var secondConfig = new Config.Builder(CONFIG, Collections.emptyList()).build();
        // Check test results
        assertEquals(firstConfig.toString(), secondConfig.toString());
    }

    @Test
    @DisplayName("Create a config via the builder")
    void createConfigViaBuilder() {
        final var firstConfig = getConfig(Collections.singletonList(getProperty()));
        final var secondConfig = new Config.Builder(firstConfig).build();
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Create a config via the json builder")
    void createConfigViaJsonBuilder() throws JsonException {
        final String json = "{\"name\":\"Config\",\"description\":\"Description\"}";
        final var firstConfig = new Config.Builder((JsonObject) Jsoner.deserialize(json)).build();
        // Check test results
        assertTrue(firstConfig.getAttributes().isPresent());
        assertEquals(0, firstConfig.getProperties().count());
    }

    @Test
    @DisplayName("Create a config with params via the json builder")
    void createConfigWithParamsViaJsonBuilder() throws JsonException {
        final var firstConfig = getConfig(Collections.singletonList(getProperty()));
        final var secondConfig =
                new Config.Builder((JsonObject) Jsoner.deserialize(firstConfig.toJson())).build();
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Convert a config to a json")
    void convertConfigToJson() throws IOException {
        final var config = getConfig(Collections.emptyList());
        final var writer = new StringWriter();
        config.toJson(writer);
        // Check test results
        assertEquals(writer.toString(), config.toJson());
    }

    @Test
    @DisplayName("Convert a config with properties to a json")
    void convertConfigWithPropertiesToJson() throws IOException {
        final var config = getConfig(Collections.singletonList(getProperty()));
        final StringWriter writer = new StringWriter();
        config.toJson(writer);
        // Check test results
        assertEquals(writer.toString(), config.toJson());
    }
}
