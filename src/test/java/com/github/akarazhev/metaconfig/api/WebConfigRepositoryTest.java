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

import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.engine.web.WebServers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WebConfigRepositoryTest {
    private static final String FIRST_CONFIG = "The First Config";
    private static final String SECOND_CONFIG = "The Second Config";

    private static WebServer webServer;
    private static ConfigRepository configRepository;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newTestServer().start();
        if (configRepository == null) {
            final Collection<Property> properties = new ArrayList<>(1);
            properties.add(new Property.Builder(URL, "https://localhost:8000/api/metacfg").build());
            properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
            final Config config = new Config.Builder(CONFIG_NAME, properties).build();

            configRepository = new WebConfigRepository.Builder(config).build();
        }
    }

    @AfterAll
    static void afterAll() {
        webServer.stop();
        webServer = null;
    }

    @Test
    void findByNames() {
        final Config[] configs = configRepository.findByNames(Stream.of("name")).toArray(Config[]::new);
        // Check test results
        assertEquals(1, configs.length);
    }

    @Test
    void findNames() {
        final String[] names = configRepository.findNames().toArray(String[]::new);
        // Check test results
        assertEquals(2, names.length);
    }

    @Test
    void saveAndFlush() {
        assertEquals(2, configRepository.saveAndFlush(Stream.of(getFirstConfig(), getSecondConfig())).count());
    }

    @Test
    void delete() {
        int count = configRepository.delete(Stream.of("name"));
        assertEquals(1, count);
    }

    private Config getFirstConfig() {
        final Property firstSubProperty = new Property.Builder("Sub-Property-1", "Sub-Value-1").
                attribute("key_1", "value_1").build();
        final Property secondSubProperty = new Property.Builder("Sub-Property-2", "Sub-Value-2").
                attribute("key_2", "value_2").build();
        final Property thirdSubProperty = new Property.Builder("Sub-Property-3", "Sub-Value-3").
                attribute("key_3", "value_3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attribute("key", "value").
                property(new String[0], firstSubProperty).
                property(new String[]{"Sub-Property-1"}, secondSubProperty).
                property(new String[]{"Sub-Property-1", "Sub-Property-2"}, thirdSubProperty).
                build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        attributes.put("key_3", "value_3");

        return new Config.Builder(FIRST_CONFIG, Collections.singletonList(property)).attributes(attributes).build();
    }

    private Config getSecondConfig() {
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").
                attribute("key_1", "value_1").build();
        final Property secondProperty = new Property.Builder("Property-2", "Value-2").
                attribute("key_2", "value_2").build();
        final Property thirdProperty = new Property.Builder("Property-3", "Value-3").
                attribute("key_3", "value_3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attribute("key", "value").
                property(new String[0], firstProperty).
                property(new String[0], secondProperty).
                property(new String[0], thirdProperty).
                build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        attributes.put("key_3", "value_3");

        return new Config.Builder(SECOND_CONFIG, Collections.singletonList(property)).attributes(attributes).build();
    }
}
