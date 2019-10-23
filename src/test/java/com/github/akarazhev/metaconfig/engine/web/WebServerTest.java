package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.engine.web.internal.constant.Constants.API.PING;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebServerTest {
    private static WebServer webServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newServer(new ConfigService() {

            @Override
            public Config update(Config config, boolean override) {
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
            public Optional<Config> get(String name) {
                return Optional.empty();
            }

            @Override
            public void remove(String name) {

            }

            @Override
            public void accept(Config config) {

            }

            @Override
            public void addConsumer(Consumer<Config> consumer) {

            }
        }).start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        webServer.stop();
    }

    @Test
    void getStatusMethod() throws Exception {
        String url = "http://localhost:8000" + PING;
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        // Test status code
        assertEquals(200, response.getStatusLine().getStatusCode());
        // Get the response
        assertEquals("ok", getJsonObject(response.getEntity().getContent()).get("status"));
    }

    private JsonObject getJsonObject(InputStream inputStream) throws JsonException {
        Scanner scanner = new Scanner(inputStream);
        StringBuilder json = new StringBuilder();
        while(scanner.hasNext()){
            json.append(scanner.nextLine());
        }

        scanner.close();
        return (JsonObject) Jsoner.deserialize(json.toString());
    }
}
