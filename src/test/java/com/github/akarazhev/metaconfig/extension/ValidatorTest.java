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
package com.github.akarazhev.metaconfig.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Validator test")
final class ValidatorTest {

    @Test
    @DisplayName("Of validator")
    void ofValidator() {
        final Object object = new Object();
        // Check test results
        assertEquals(object, Validator.of(object).get());
    }

    @Test
    @DisplayName("Validate success")
    void validateSuccess() {
        final Object object = new Object();
        // Check test results
        assertEquals(object, Validator.of(object).validate(o -> true, "success").get());
    }

    @Test
    @DisplayName("Validate not success")
    void validateNotSuccess() {
        final Object object = new Object();
        // Check test results
        assertThrows(IllegalStateException.class,
                () -> Validator.of(object).validate(o -> false, "not success").get());
    }

    @Test
    @DisplayName("Validate projected success")
    void validateProjectedSuccess() {
        final Object object = new Object();
        // Check test results
        assertEquals(object, Validator.of(object).validate(o -> true, o -> true, "success").get());
    }

    @Test
    @DisplayName("Validate Projected not success")
    void validateProjectedNotSuccess() {
        final Object object = new Object();
        // Check test results
        assertThrows(IllegalStateException.class,
                () -> Validator.of(object).validate(o -> false, o -> false, "not success").get());
    }
}
