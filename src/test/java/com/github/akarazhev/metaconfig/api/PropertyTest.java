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

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PropertyTest {

    @Test
    @DisplayName("Create a simple property")
    void createSimpleProperty() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").build();
        // Check test results
        assertEquals("Simple Property", property.getName());
        assertEquals("Simple Value", property.getValue());
        assertThrows(ClassCastException.class, property::asBool);
        assertThrows(ClassCastException.class, property::asDouble);
        assertThrows(ClassCastException.class, property::asLong);
        assertThrows(ClassCastException.class, property::asArray);
        assertEquals(1, property.getVersion());
        assertFalse(property.getCaption().isPresent());
        assertFalse(property.getDescription().isPresent());
        assertTrue(property.getAttributes().isPresent());
        assertTrue(property.getAttributes().get().isEmpty());
        assertFalse(property.getAttribute("key").isPresent());
        assertEquals(0, property.getAttributeKeys().count());
    }

    @Test
    @DisplayName("Create a property with parameters")
    void createPropertyWithParameters() {
        final Property firstSubProperty = new Property.Builder("Sub-Property-1", "Sub-Value-1").build();
        final Property secondSubProperty = new Property.Builder("Sub-Property-2", "Sub-Value-2").build();
        final Property thirdSubProperty = new Property.Builder("Sub-Property-3", "Sub-Value-3").build();
        final Property property = new Property.Builder("Property", "Value").
                caption("Caption").
                description("Description").
                attributes(Collections.singletonMap("key", "value")).
                property(new String[0], firstSubProperty).
                properties(new String[0], Collections.singletonList(secondSubProperty)).
                properties(Collections.singletonList(thirdSubProperty)).
                build();
        // Check test results
        assertEquals("Property", property.getName());
        assertEquals("Value", property.getValue());
        assertEquals(1, property.getVersion());
        assertTrue(property.getCaption().isPresent());
        assertEquals("Caption", property.getCaption().get());
        assertTrue(property.getDescription().isPresent());
        assertEquals("Description", property.getDescription().get());
        assertTrue(property.getAttributes().isPresent());
        assertEquals(1, property.getAttributes().get().size());
        assertEquals(1, property.getAttributeKeys().count());
        assertTrue(property.getAttribute("key").isPresent());
        assertEquals(3, property.getProperties().count());
        assertTrue(property.getProperty("Sub-Property-1").isPresent());
        assertTrue(property.getProperty("Sub-Property-2").isPresent());
        assertTrue(property.getProperty("Sub-Property-3").isPresent());
    }

    @Test
    @DisplayName("Create a simple property with property by the empty path")
    void createSimplePropertyWithPropertyByEmptyPath() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").
                property(new String[0], new Property.Builder("Sub-property", "Sub-value").build()).build();
        // Check test results
        assertEquals("Simple Property", property.getName());
        assertEquals("Simple Value", property.getValue());
        final Optional<Property> subProperty = property.getProperty("Sub-property");
        assertTrue(subProperty.isPresent());
        assertEquals("Sub-property", subProperty.get().getName());
    }

    @Test
    @DisplayName("Create a simple property with property by the single path")
    void createSimplePropertyWithPropertyBySinglePath() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").
                property(new String[]{"Sub-property-1"},
                        new Property.Builder("Sub-property-2", "Sub-value-2").build()).build();
        // Check test results
        assertEquals("Simple Property", property.getName());
        assertEquals("Simple Value", property.getValue());
        // Check Sub-property-1
        final Optional<Property> firstSubProperty = property.getProperty("Sub-property-1");
        assertTrue(firstSubProperty.isPresent());
        assertEquals("Sub-property-1", firstSubProperty.get().getName());
        // Check Sub-property-2
        final Optional<Property> secondSubProperty = firstSubProperty.get().getProperty("Sub-property-2");
        assertTrue(secondSubProperty.isPresent());
        assertEquals("Sub-property-2", secondSubProperty.get().getName());
    }

    @Test
    @DisplayName("Create a simple property with property by the multiple path")
    void createSimplePropertyWithPropertyByMultiplePath() {
        final Property property = new Property.Builder("Simple Property", "Simple Value").
                property(new String[]{"Sub-property-1", "Sub-property-2"},
                        new Property.Builder("Sub-property-3", "Sub-value-3").build()).build();
        // Check test results
        assertEquals("Simple Property", property.getName());
        assertEquals("Simple Value", property.getValue());
        // Check Sub-property-1
        final Optional<Property> firstSubProperty = property.getProperty("Sub-property-1");
        assertTrue(firstSubProperty.isPresent());
        assertEquals("Sub-property-1", firstSubProperty.get().getName());
        // Check Sub-property-2
        final Optional<Property> secondSubProperty = firstSubProperty.get().getProperty("Sub-property-2");
        assertTrue(secondSubProperty.isPresent());
        assertEquals("Sub-property-2", secondSubProperty.get().getName());
        // Check Sub-property-3
        final Optional<Property> thirdSubProperty = secondSubProperty.get().getProperty("Sub-property-3");
        assertTrue(thirdSubProperty.isPresent());
        assertEquals("Sub-property-3", thirdSubProperty.get().getName());
    }

    @Test
    @DisplayName("Create a simple bool property")
    void createSimpleBoolProperty() {
        final Property property = new Property.Builder("Simple Property", true).build();
        // Check test results
        assertTrue(property.asBool());
    }

    @Test
    @DisplayName("Create a simple double property")
    void createSimpleDoubleProperty() {
        final Property property = new Property.Builder("Simple Property", 0.0).build();
        // Check test results
        assertEquals(0.0, property.asDouble());
    }

    @Test
    @DisplayName("Create a simple long property")
    void createSimpleLongProperty() {
        final Property property = new Property.Builder("Simple Property", 0L).build();
        // Check test results
        assertEquals(0L, property.asLong());
    }

    @Test
    @DisplayName("Create a simple array property")
    void createSimpleArrayProperty() {
        final Property property = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        // Check test results
        assertEquals(new String[]{"Simple Value"}[0], property.asArray()[0]);
    }

    @Test
    @DisplayName("Compare two properties")
    void compareTwoProperties() {
        final Property firstProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        final Property secondProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        // Check test results
        assertEquals(firstProperty, secondProperty);
    }

    @Test
    @DisplayName("Check hash codes of two properties")
    void checkHashCodesOfTwoProperties() {
        final Property firstProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        final Property secondProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        // Check test results
        assertEquals(firstProperty.hashCode(), secondProperty.hashCode());
    }

    @Test
    @DisplayName("Check toString() of two properties")
    void checkToStringOfTwoProperties() {
        final Property firstProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        final Property secondProperty = new Property.Builder("Simple Property", new String[]{"Simple Value"}).build();
        // Check test results
        assertEquals(firstProperty.toString(), secondProperty.toString());
    }
}
