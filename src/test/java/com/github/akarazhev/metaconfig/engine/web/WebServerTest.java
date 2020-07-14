/* Copyright 2019-2020 Andrey Karazhev
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
import com.github.akarazhev.metaconfig.api.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.SUCCESS;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Web server test")
final class WebServerTest {

    @Test
    @DisplayName("Start")
    void start() throws Exception {
        final WebServer webServer = WebServers.newTestServer().start();
        assertGetConfigNames();
        webServer.stop();
    }

    @Test
    @DisplayName("Stop")
    void stop() throws Exception {
        final WebServer webServer = WebServers.newTestServer().start();
        assertGetConfigNames();
        webServer.stop();
        assertThrows(RuntimeException.class, this::assertGetConfigNames);
    }

    private void assertGetConfigNames() throws Exception {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());
        properties.add(new Property.Builder(URL, "https://localhost:8000/api/metacfg/config_names").build());
        properties.add(new Property.Builder(METHOD, GET).build());

        final Config config = new Config.Builder(CONFIG_NAME, properties).build();
        final WebClient client = new WebClient.Builder(config).build();
        // Test status code
        assertEquals(HTTP_OK, client.getStatusCode());
        // Get the response
        assertEquals(true, client.getJsonContent().get(SUCCESS));
    }
}
