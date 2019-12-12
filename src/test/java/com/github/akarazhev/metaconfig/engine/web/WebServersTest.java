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

import com.github.akarazhev.metaconfig.UnitTest;
import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.extension.URLUtils;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import static com.github.akarazhev.metaconfig.Constants.Messages.JSON_TO_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.METHOD_NOT_ALLOWED;
import static com.github.akarazhev.metaconfig.Constants.Messages.REQUEST_PARAM_NOT_PRESENT;
import static com.github.akarazhev.metaconfig.Constants.Messages.STRING_TO_JSON_ERROR;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Header.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.POST;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.PUT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_CONFIG_ENDPOINT_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_ENDPOINT_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAMES_ENDPOINT_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.ERROR;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.RESULT;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.SUCCESS;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Web servers test")
final class WebServersTest extends UnitTest {
    private static final String API_URL = "https://localhost:8000/api/metacfg";
    private static WebServer webServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newTestServer().start();
    }

    @AfterAll
    static void afterAll() {
        webServer.stop();
        webServer = null;
    }

    @Test
    @DisplayName("Web servers constructor")
    void webServersConstructor() throws Exception {
        assertPrivate(WebServers.class);
    }

    @Test
    @DisplayName("Accept a config")
    void acceptConfig() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + ACCEPT_CONFIG_ENDPOINT_VALUE + "/" +
                URLUtils.encode("name_1", StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, POST).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get(SUCCESS));
    }

    @Test
    @DisplayName("Accept a config with a wrong method")
    void acceptConfigWrongMethod() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + ACCEPT_CONFIG_ENDPOINT_VALUE + "/" +
                URLUtils.encode("name_1", StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_BAD_METHOD, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(METHOD_NOT_ALLOWED, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Get config names")
    void getConfigNames() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_NAMES_ENDPOINT_VALUE).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get(SUCCESS));
    }

    @Test
    @DisplayName("Get config names with a wrong method")
    void getConfigNamesWrongMethod() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_NAMES_ENDPOINT_VALUE).build());
        properties.add(new Property.Builder(METHOD, POST).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_BAD_METHOD, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(METHOD_NOT_ALLOWED, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Get configs")
    void getConfigs() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get(SUCCESS));
    }

    @Test
    @DisplayName("Get configs by names")
    void getConfigsByNames() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE + "?names=" +
                new String(Base64.getEncoder().encode("[\"name_1\", \"name_2\", \"name_3\"]".getBytes()),
                        StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get(SUCCESS));
    }

    @Test
    @DisplayName("Get configs by names not encoded")
    void getConfigsByNamesNotEncoded() throws JsonException {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE + "?names=" +
                URLUtils.encode("[\"name_1\", \"name_2\", \"name_3\"]", StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(STRING_TO_JSON_ERROR, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Get configs by names not in the json format")
    void getConfigsByNamesNotJsonFormat() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE + "?names=" +
                URLUtils.encode("[name_1, name_2, name_3]", StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(STRING_TO_JSON_ERROR, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Get configs with a wrong method")
    void getConfigsWrongMethod() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE).build());
        properties.add(new Property.Builder(METHOD, POST).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_BAD_METHOD, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(METHOD_NOT_ALLOWED, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Update a config")
    void updateConfig() throws Exception {
        final Collection<Property> properties = new ArrayList<>(2);
        properties.add(new Property.Builder("Property_1", "Value_1").build());
        properties.add(new Property.Builder("Property_2", "Value_2").build());
        Config config = new Config.Builder("Meta Config", properties).
                attributes(Collections.singletonMap("key", "value")).build();

        final Collection<Property> props = new ArrayList<>(6);
        props.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        props.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE).build());
        props.add(new Property.Builder(METHOD, PUT).build());
        props.add(new Property.Builder(ACCEPT, APPLICATION_JSON).build());
        props.add(new Property.Builder(CONTENT_TYPE, APPLICATION_JSON).build());
        props.add(new Property.Builder(CONTENT, Jsoner.serialize(new Config[]{config})).build());

        config = new Config.Builder(CONFIG_NAME, props).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(true, jsonObject.get(SUCCESS));
    }

    @Test
    @DisplayName("Update a config not in the json format")
    void updateConfigNotJsonFormat() throws Exception {
        final Collection<Property> properties = new ArrayList<>(2);
        properties.add(new Property.Builder("Property_1", "Value_1").build());
        properties.add(new Property.Builder("Property_2", "Value_2").build());
        Config config = new Config.Builder("Meta Config", properties).
                attributes(Collections.singletonMap("key", "value")).build();

        final Collection<Property> props = new ArrayList<>(6);
        props.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        props.add(new Property.Builder(URL, API_URL + "/config").build());
        props.add(new Property.Builder(METHOD, PUT).build());
        props.add(new Property.Builder(ACCEPT, APPLICATION_JSON).build());
        props.add(new Property.Builder(CONTENT_TYPE, APPLICATION_JSON).build());
        props.add(new Property.Builder(CONTENT, config.toString()).build());

        config = new Config.Builder(CONFIG_NAME, props).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_BAD_REQUEST, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(false, jsonObject.get(SUCCESS));
        assertEquals(JSON_TO_CONFIG_ERROR, jsonObject.get(ERROR));
    }

    @Test
    @DisplayName("Delete a configs by names")
    void deleteConfigsByNames() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE + "?names=" +
                new String(Base64.getEncoder().encode("[\"name\"]".getBytes()), StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, DELETE).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonObject = client.getJsonContent();
        assertEquals(true, jsonObject.get(SUCCESS));
        assertEquals(0, ((BigDecimal) jsonObject.get(RESULT)).intValue());
    }

    @Test
    @DisplayName("Delete a config")
    void deleteConfig() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE).build());
        properties.add(new Property.Builder(METHOD, DELETE).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(REQUEST_PARAM_NOT_PRESENT, jsonContent.get(ERROR));
    }

    @Test
    @DisplayName("Delete a config not in the json format")
    void deleteConfigNotJsonFormat() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, API_URL + "/" + CONFIG_ENDPOINT_VALUE + "?names=" +
                new String(Base64.getEncoder().encode("[name]".getBytes()), StandardCharsets.UTF_8)).build());
        properties.add(new Property.Builder(METHOD, DELETE).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        final JsonObject jsonContent = client.getJsonContent();
        assertEquals(false, jsonContent.get(SUCCESS));
        assertEquals(STRING_TO_JSON_ERROR, jsonContent.get(ERROR));
    }
}
