package com.github.akarazhev.metaconfig.engine.web;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Consumer;

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
            public Collection<String> getNames() {
                return null;
            }

            @Override
            public Collection<Config> get() {
                return null;
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
        String url = "http://localhost:8000/api/config/status";
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);

        assertEquals(200, response.getStatusLine().getStatusCode());
    }
}
