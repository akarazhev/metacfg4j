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
package com.github.akarazhev.metaconfig.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("URL utils test")
final class URLUtilsTest {

    @Test
    @DisplayName("Encode")
    void encode() {
        // Check test results
        assertEquals("name", URLUtils.encode("name", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode error")
    void encodeError() {
        // Check test results
        assertThrows(RuntimeException.class, () -> URLUtils.encode("name", null));
    }

    @Test
    @DisplayName("Decode")
    void decode() {
        // Check test results
        assertEquals("name", URLUtils.decode("name", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Decode error")
    void decodeError() {
        // Check test results
        assertThrows(RuntimeException.class, () -> URLUtils.decode("name", null));
    }
}
