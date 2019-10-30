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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PropertyTest {

    @Test
    @DisplayName("Create a simple property")
    void createSimpleProperty() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").build();
        // Check results
        assertEquals("Simple Property", property.getName(), "The name must be: 'Simple Property'");
        assertEquals("Simple Value", property.getValue(), "The name must be: 'Simple Value'");
    }

    @Test
    @DisplayName("Create a simple property with property by the path")
    void createSimplePropertyWithPropertyByPath() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").
                property(new String[0], new Property.Builder("Sub-property", "Sub-value").build()).build();
        // Check results
        assertEquals("Simple Property", property.getName(), "The name must be: 'Simple Property'");
        assertEquals("Simple Value", property.getValue(), "The name must be: 'Simple Value'");
        final Optional<Property> subProperty = property.getProperty("Sub-property");
        assertTrue(subProperty.isPresent(), "The sub-property should be existed");
        assertEquals("Sub-property", subProperty.get().getName(), "The name must be: 'Sub-property'");
    }
}
