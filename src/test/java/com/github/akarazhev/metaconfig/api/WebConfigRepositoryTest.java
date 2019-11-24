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

import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WebConfigRepositoryTest {
    private static WebServer webServer;
    private static ConfigRepository configRepository;

    @BeforeAll
    static void beforeAll() throws Exception {
        webServer = WebServers.newServer().start();
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
    void findNames() {
        final String[] names = configRepository.findNames().toArray(String[]::new);
        // Check test results
        assertEquals(1, names.length);
    }
}
