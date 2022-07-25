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

import com.github.akarazhev.metaconfig.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Operation response test")
final class OperationResponseTest extends UnitTest {

    @Test
    @DisplayName("Fields constructor")
    void fieldsConstructor() throws Exception {
        assertPrivate(OperationResponse.Fields.class);
    }

    @Test
    @DisplayName("Result is success")
    void resultIsSuccess() {
        final OperationResponse<String> response = new OperationResponse.Builder<String>().result("Ok").build();
        // Check test results
        assertTrue(response.isSuccess());
        assertEquals("Ok", response.getResult());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("Result is not success")
    void resultIsNotSuccess() {
        final OperationResponse<String> response = new OperationResponse.Builder<String>().error("Error").build();
        // Check test results
        assertFalse(response.isSuccess());
        assertEquals("Error", response.getError());
        assertNull(response.getResult());
    }
}
