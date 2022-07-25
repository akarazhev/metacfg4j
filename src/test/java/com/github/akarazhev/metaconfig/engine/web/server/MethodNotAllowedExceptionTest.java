/* Copyright 2019-2022 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Method not allowed exception test")
final class MethodNotAllowedExceptionTest {

    @Test
    @DisplayName("Method not allowed exception")
    void methodNotAllowedException() {
        assertThrows(MethodNotAllowedException.class, () -> {throw new MethodNotAllowedException(0, "Error");});
    }
}
