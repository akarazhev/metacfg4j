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
package com.github.akarazhev.metaconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Constants test")
final class ConstantsTest extends UnitTest {

    @Test
    @DisplayName("Constants constructor")
    void constantsConstructor() throws Exception {
        assertPrivate(Constants.class);
    }

    @Test
    @DisplayName("Mapping constructor")
    void mappingConstructor() throws Exception {
        assertPrivate(Constants.Mapping.class);
    }
    @Test
    @DisplayName("Endpoints constructor")
    void endpointsConstructor() throws Exception {
        assertPrivate(Constants.Endpoints.class);
    }

    @Test
    @DisplayName("Messages constructor")
    void messagesConstructor() throws Exception {
        assertPrivate(Constants.Messages.class);
    }
}
