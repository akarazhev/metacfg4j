package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebServerTest {
    private static WebServer webServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newServer(new ConfigService() {

            @Override
            public Config update(final Config config, final boolean override) {
                return null;
            }

            @Override
            public Stream<String> getNames() {
                return Stream.empty();
            }

            @Override
            public Stream<Config> get() {
                return Stream.empty();
            }

            @Override
            public Optional<Config> get(final String name) {
                return Optional.empty();
            }

            @Override
            public void remove(final String name) {

            }

            @Override
            public void accept(final String name) {

            }

            @Override
            public void addConsumer(final Consumer<Config> consumer) {

            }
        }).start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        webServer.stop();
    }

    @Test
    void acceptConfig() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpPost("http://localhost:8000/api/config/accept/name"));
        // Test status code
        assertEquals(200, response.getStatusLine().getStatusCode());
        // Get the response
        assertEquals(true, getJsonObject(response.getEntity().getContent()).get("success"));
    }

    @Test
    void getConfigNames() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet("http://localhost:8000/api/config/names"));
        // Test status code
        assertEquals(200, response.getStatusLine().getStatusCode());
        // Get the response
        assertEquals(true, getJsonObject(response.getEntity().getContent()).get("success"));
    }

    @Test
    void getConfigSections() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet("http://localhost:8000/api/config/sections?names=" +
                new String(Base64.getEncoder().encode("[\"name_1\", \"name_2\", \"name_3\"]".getBytes()))));
        // Test status code
        assertEquals(200, response.getStatusLine().getStatusCode());
        // Get the response
        JsonObject jsonObject = getJsonObject(response.getEntity().getContent());
        assertEquals(true, jsonObject.get("success"));
    }

    @Test
    void getConfigSection() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet("http://localhost:8000/api/config/section/name"));
        // Test status code
        assertEquals(200, response.getStatusLine().getStatusCode());
        // Get the response
        JsonObject jsonObject = getJsonObject(response.getEntity().getContent());
        assertEquals(false, jsonObject.get("success"));
        assertEquals("Section not found", jsonObject.get("error"));
    }

    private JsonObject getJsonObject(final InputStream inputStream) throws JsonException {
        final Scanner scanner = new Scanner(inputStream);
        final StringBuilder json = new StringBuilder();
        while(scanner.hasNext()){
            json.append(scanner.nextLine());
        }

        scanner.close();
        return (JsonObject) Jsoner.deserialize(json.toString());
    }
}
