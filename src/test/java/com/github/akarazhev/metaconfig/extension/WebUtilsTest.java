/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.extension;

import com.github.akarazhev.metaconfig.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("URL utils test")
final class WebUtilsTest extends UnitTest {

    @Test
    @DisplayName("URL utils constructor")
    void urlUtilsConstructor() throws Exception {
        assertPrivate(WebUtils.class);
    }

    @Test
    @DisplayName("Encode")
    void encode() {
        // Check test results
        assertEquals("name", WebUtils.encode("name", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode error")
    void encodeError() {
        // Check test results
        assertThrows(RuntimeException.class, () -> WebUtils.encode("name", null));
    }

    @Test
    @DisplayName("Decode")
    void decode() {
        // Check test results
        assertEquals("name", WebUtils.decode("name", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Decode error")
    void decodeError() {
        // Check test results
        assertThrows(RuntimeException.class, () -> WebUtils.decode("name", null));
    }

    @Test
    @DisplayName("Get path params")
    void getPathParams() {
        final Stream<String> params = WebUtils.getPathParams(URI.create("path"), "api");
        // Check test results
        assertEquals(0, params.count());
    }

    @Test
    @DisplayName("Get request param")
    void getRequestParam() {
        final Optional<String> param = WebUtils.getRequestParam(URI.create("http://path?path"), "api");
        // Check test results
        assertFalse(param.isPresent());
    }
}
