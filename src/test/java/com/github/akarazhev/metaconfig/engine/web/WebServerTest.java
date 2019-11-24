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
package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.api.Property;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static com.github.akarazhev.metaconfig.engine.web.Constants.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.Constants.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.POST;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.PUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WebServerTest {
    private static final String API_URL = "https://localhost:8000/api/metacfg";
    private static WebServer webServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newServer(new ConfigService() {
            private Consumer<Config> consumer;
            private final Map<String, Config> map = new HashMap<String, Config>() {{
                put("name", new Config.Builder("name", Collections.singletonList(
                        new Property.Builder("name", "value").build())).build());
            }};

            @Override
            public Stream<Config> update(final Stream<Config> stream) {
                final Collection<Config> list = stream.collect(Collectors.toList());
                for (final Config config : list) {
                    map.put(config.getName(), config);
                }

                return list.stream();
            }

            @Override
            public Stream<String> getNames() {
                return map.keySet().stream();
            }

            @Override
            public Stream<Config> get() {
                return map.values().stream();
            }

            @Override
            public Stream<Config> get(final Stream<String> stream) {
                final Collection<Config> configs = new LinkedList<>();
                stream.forEach(name -> configs.add(map.get(name)));
                return configs.stream();
            }

            @Override
            public int remove(final Stream<String> stream) {
                int size = map.size();
                stream.forEach(map::remove);
                return size - map.size();
            }

            @Override
            public void accept(final String name) {
                if (consumer != null) {
                    get(Stream.of(name)).findAny().ifPresent(config -> consumer.accept(config));
                }
            }

            @Override
            public void addConsumer(final Consumer<Config> consumer) {
                this.consumer = consumer;
            }
        }).start();
    }

    @AfterAll
    static void afterAll() {
        webServer.stop();
        webServer = null;
    }

    @Test
    void acceptConfig() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/accept_config/name").build());
        properties.add(new Property.Builder(METHOD, POST).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get("success"));
    }

    @Test
    void getConfigNames() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/config_names").build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get("success"));
    }

    @Test
    void getConfigSections() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/configs?names=" +
                new String(Base64.getEncoder().encode("[\"name_1\", \"name_2\", \"name_3\"]".getBytes()))).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get("success"));
    }

    @Test
    void getConfigSection() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/config/name").build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(true, jsonObject.get("success"));
    }

    @Test
    void updateConfigSection() throws Exception {
        final Collection<Property> properties = new ArrayList<>(2);
        properties.add(new Property.Builder("Property_1", "Value_1").build());
        properties.add(new Property.Builder("Property_2", "Value_2").build());
        Config config = new Config.Builder("Meta Config", properties).attributes(Collections.singletonMap("key", "value")).build();

        final Collection<Property> props = new ArrayList<>(6);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/config").build());
        properties.add(new Property.Builder(METHOD, PUT).build());
        properties.add(new Property.Builder(ACCEPT, APPLICATION_JSON).build());
        properties.add(new Property.Builder(CONTENT_TYPE, APPLICATION_JSON).build());
        properties.add(new Property.Builder(CONTENT, Jsoner.serialize(new Config[]{config})).build());

        config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(true, jsonObject.get("success"));
    }

    @Test
    void deleteConfigSection() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/config/" +
                new String(Base64.getEncoder().encode("[\"name\"]".getBytes()))).build());
        properties.add(new Property.Builder(METHOD, DELETE).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(200, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(true, jsonObject.get("success"));
        assertEquals(1, ((BigDecimal) jsonObject.get("result")).intValue());
    }
}
