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
        final Config config = new Config.Builder("Config", Collections.emptyList()).build();
        // Check test results
        assertEquals("Config", config.getName());
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
                () -> new Config.Builder("Config", Collections.emptyList()).id(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> new Config.Builder("Config", Collections.emptyList()).version(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> new Config.Builder("Config", Collections.emptyList()).updated(0).build());
    }

    @Test
    @DisplayName("Create a config with properties")
    void createConfigWithParameters() {
        final Config config = getConfig(Collections.singletonList(getProperty()));
        // Check test results
        assertEquals(100, config.getId());
        assertEquals(CONFIG_NAME, config.getName());
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
        final Config firstConfig = getConfig(Collections.emptyList());
        // Check test results
        assertEquals(firstConfig, firstConfig);
    }

    @Test
    @DisplayName("Compare two simple configs")
    void compareTwoSimpleConfigs() {
        final Config firstConfig = getConfig(Collections.emptyList());
        final Config secondConfig = getConfig(Collections.emptyList());
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Compare two configs")
    void compareTwoConfigs() {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").build();
        final Config firstConfig = new Config.Builder(CONFIG_NAME, Collections.singletonList(firstProperty)).
                id(1).
                description("Description").
                version(1).
                updated(UPDATED).
                attribute("key_1", "value_1").
                attributes(Collections.singletonMap("key_2", "value_2")).
                property(new String[0], new Property.Builder("Property-2", "Value-2").build()).
                build();
        final Property secondProperty = new Property.Builder("Property-1", "Value-1").build();
        final Config secondConfig = new Config.Builder(CONFIG_NAME, Collections.singletonList(secondProperty)).
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
        final Config firstConfig = new Config.Builder(CONFIG_NAME, Collections.emptyList()).build();
        final Config secondConfig = new Config.Builder(CONFIG_NAME, Collections.emptyList()).build();
        // Check test results
        assertEquals(firstConfig.hashCode(), secondConfig.hashCode());
    }

    @Test
    @DisplayName("Check toString() of two configs")
    void checkToStringOfTwoConfigs() {
        final Config firstConfig = new Config.Builder(CONFIG_NAME, Collections.emptyList()).build();
        final Config secondConfig = new Config.Builder(CONFIG_NAME, Collections.emptyList()).build();
        // Check test results
        assertEquals(firstConfig.toString(), secondConfig.toString());
    }

    @Test
    @DisplayName("Create a config via the builder")
    void createConfigViaBuilder() {
        final Config firstConfig = getConfig(Collections.singletonList(getProperty()));
        final Config secondConfig = new Config.Builder(firstConfig).build();
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Create a config via the json builder")
    void createConfigViaJsonBuilder() throws JsonException {
        final String json = "{\"name\":\"Config\",\"description\":\"Description\"}";
        final Config firstConfig = new Config.Builder((JsonObject) Jsoner.deserialize(json)).build();
        // Check test results
        assertTrue(firstConfig.getAttributes().isPresent());
        assertEquals(0, firstConfig.getProperties().count());
    }

    @Test
    @DisplayName("Create a config with params via the json builder")
    void createConfigWithParamsViaJsonBuilder() throws JsonException {
        final Config firstConfig = getConfig(Collections.singletonList(getProperty()));
        final Config secondConfig =
                new Config.Builder((JsonObject) Jsoner.deserialize(firstConfig.toJson())).build();
        // Check test results
        assertEquals(firstConfig, secondConfig);
    }

    @Test
    @DisplayName("Convert a config to a json")
    void convertConfigToJson() throws IOException {
        final Config config = getConfig(Collections.emptyList());
        final StringWriter writer = new StringWriter();
        config.toJson(writer);
        // Check test results
        assertEquals(writer.toString(), config.toJson());
    }

    @Test
    @DisplayName("Convert a config with properties to a json")
    void convertConfigWithPropertiesToJson() throws IOException {
        final Config config = getConfig(Collections.singletonList(getProperty()));
        final StringWriter writer = new StringWriter();
        config.toJson(writer);
        // Check test results
        assertEquals(writer.toString(), config.toJson());
    }
}
