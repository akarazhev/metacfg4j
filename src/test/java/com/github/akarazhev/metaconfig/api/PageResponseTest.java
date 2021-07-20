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

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Page response test")
final class PageResponseTest extends UnitTest {

    @Test
    @DisplayName("Create a page response")
    void createPageRequest() {
        final PageResponse response = new PageResponse.Builder(Collections.singletonList(CONFIG)).
                page(0).
                total(10).
                build();
        // Check test results
        assertEquals(CONFIG, response.getNames().toArray(String[]::new)[0]);
        assertEquals(0, response.getPage());
        assertEquals(10, response.getTotal());
    }

    @Test
    @DisplayName("Create a page response via the json builder")
    void createPageResponseViaJsonBuilder() throws JsonException {
        final String json = "{\"total\":10,\"names\":[\"Config\"],\"page\":1}";
        final PageResponse response = new PageResponse.Builder((JsonObject) Jsoner.deserialize(json)).build();
        // Check test results
        assertEquals(CONFIG, response.getNames().toArray(String[]::new)[0]);
        assertEquals(1, response.getPage());
        assertEquals(10, response.getTotal());
    }

    @Test
    @DisplayName("Check toString() of two page responses")
    void checkToStringOfTwoResponses() {
        final PageResponse firstResponse = new PageResponse.Builder(Collections.singletonList(CONFIG)).build();
        final PageResponse secondResponse = new PageResponse.Builder(Collections.singletonList(CONFIG)).build();
        // Check test results
        assertEquals(firstResponse.toString(), secondResponse.toString());
    }

    @Test
    @DisplayName("Create a page response exception")
    void createPageResponseException() {
        final Collection<String> names = null;
        // Check test results
        assertThrows(NullPointerException.class, () -> new PageResponse.Builder(names));
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"total\":-1,\"page\":-1}";
            new PageResponse.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"total\":-1,\"names\":[\"Config\"]}";
            new PageResponse.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            final String json = "{\"names\":[\"Config\"],\"page\":-1}";
            new PageResponse.Builder((JsonObject) Jsoner.deserialize(json));
        });
        assertThrows(IllegalArgumentException.class, () ->
                new PageResponse.Builder(Collections.singletonList(CONFIG)).total(-1));
        assertThrows(IllegalArgumentException.class, () ->
                new PageResponse.Builder(Collections.singletonList(CONFIG)).page(-1));
    }
}
