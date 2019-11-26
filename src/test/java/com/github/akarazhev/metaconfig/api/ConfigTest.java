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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigTest {
    private final long UPDATED = Clock.systemDefaultZone().millis();

    @Test
    @DisplayName("Create a config")
    void createConfig() {
        final Config config = new Config.Builder("Config", Collections.emptyList()).build();
        assertEquals("Config", config.getName());
        assertFalse(config.getDescription().isPresent());
        assertEquals(1, config.getVersion());
        assertTrue(config.getUpdated() > 0);
        assertTrue(config.getAttributes().isPresent());
        assertTrue(config.getAttributes().get().isEmpty());
        assertFalse(config.getAttribute("key").isPresent());
        assertEquals(0, config.getAttributeKeys().count());
    }

    private Property getProperty() {
        return new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attribute("key", "value").
                property(new String[0], new Property.Builder("Sub-property-1", "Sub-value-1").build()).
                build();
    }

    private Config getConfig() {
        return new Config.Builder("Config", Collections.singletonList(getProperty())).
                id(1).
                description("Description").
                version(1).
                updated(UPDATED).
                attribute("key", "value").
                property(new String[0], new Property.Builder("Sub-property-1", "Sub-value-1").build()).
                build();
    }
}
