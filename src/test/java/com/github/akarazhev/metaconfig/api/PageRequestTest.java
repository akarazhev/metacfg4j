/* Copyright 2019-2021 Andrey Karazhev
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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Page request test")
final class PageRequestTest extends UnitTest {

    @Test
    @DisplayName("Create a page request")
    void createPageRequest() {
        final var request = new PageRequest.Builder(CONFIG).
                attribute("key_1", "value_1").
                attributes(Collections.singletonMap("key_2", "value_2")).
                page(0).
                size(10).
                ascending(true).
                build();
        // Check test results
        assertEquals(CONFIG, request.getName());
        assertEquals(0, request.getPage());
        assertEquals(10, request.getSize());
        assertTrue(request.isAscending());
        assertEquals(2, request.getAttributes().size());
    }

    @Test
    @DisplayName("Create a page request via the json builder")
    void createPageRequestViaJsonBuilder() throws JsonException {
        final var json = "{\"size\":10,\"name\":\"Config\",\"attributes\":{\"key_2\":\"value_2\"," +
                "\"key_1\":\"value_1\"},\"page\":1,\"ascending\":true}";
        final var request = new PageRequest.Builder((JsonObject) Jsoner.deserialize(json)).build();
        // Check test results
        assertEquals(CONFIG, request.getName());
        assertEquals(1, request.getPage());
        assertEquals(10, request.getSize());
        assertTrue(request.isAscending());
        assertEquals(2, request.getAttributes().size());
    }

    @Test
    @DisplayName("Check toString() of two page requests")
    void checkToStringOfTwoRequests() {
        final var firstRequest = new PageRequest.Builder(CONFIG).build();
        final var secondRequest = new PageRequest.Builder(CONFIG).build();
        // Check test results
        assertEquals(firstRequest.toString(), secondRequest.toString());
    }

    @Test
    @DisplayName("Create a page request exception")
    void createPageRequestException() {
        // Check test results
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"size\":-1,\"name\":\"Config\",\"page\":1,\"ascending\":true}";
            new PageRequest.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"name\":\"Config\",\"page\":-1,\"ascending\":true}";
            new PageRequest.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"name\":\"Config\"}";
            new PageRequest.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () -> new PageRequest.Builder(CONFIG).page(-1));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest.Builder(CONFIG).size(-1));
    }
}
