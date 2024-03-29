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
package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.UnitTest;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Property test")
final class PropertyTest extends UnitTest {

    @Test
    @DisplayName("Create a property")
    void createProperty() {
        final Property property = new Property.Builder("Property", "Value").
                id(200).
                build();
        // Check test results
        assertEquals(200, property.getId());
        assertEquals("Property", property.getName());
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
        assertTrue(property.getUpdated() > 0);
        assertThrows(ClassCastException.class, property::asBool);
        assertThrows(ClassCastException.class, property::asDouble);
        assertThrows(ClassCastException.class, property::asLong);
        assertThrows(ClassCastException.class, property::asArray);
        assertFalse(property.getCaption().isPresent());
        assertFalse(property.getDescription().isPresent());
        assertTrue(property.getAttributes().isPresent());
        assertTrue(property.getAttributes().get().isEmpty());
        assertFalse(property.getAttribute("key").isPresent());
        assertEquals(0, property.getAttributeKeys().count());
    }

    @Test
    @DisplayName("Create a property exception")
    void createPropertyException() {
        // Check test results
        assertThrows(IllegalArgumentException.class,
                () -> new Property.Builder("Value", 0).id(0));
        assertThrows(IllegalArgumentException.class,
                () -> new Property.Builder("Value", 0).updated(0));
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
                properties(new String[0], Collections.singletonList(thirdSubProperty)).
                build();
        // Check test results
        assertEquals("Property", property.getName());
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
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
    @DisplayName("Create a property with property by the empty path")
    void createPropertyWithPropertyByEmptyPath() {
        final Property property = new Property.Builder("Property", "Value").
                property(new String[0], new Property.Builder("Sub-property", "Sub-value").build()).build();
        // Check test results
        assertEquals("Property", property.getName());
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
        final Optional<Property> subProperty = property.getProperty("Sub-property");
        assertTrue(subProperty.isPresent());
        assertEquals("Sub-property", subProperty.get().getName());
    }

    @Test
    @DisplayName("Create a property with property by the single path")
    void createPropertyWithPropertyBySinglePath() {
        final Property property = new Property.Builder("Property", "Value").
                property(new String[]{"Sub-property-1"},
                        new Property.Builder("Sub-property-2", "Sub-value-2").build()).build();
        // Check test results
        assertEquals("Property", property.getName());
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
        // Check Sub-property-1
        final Optional<Property> firstSubProperty = property.getProperty("Sub-property-1");
        assertTrue(firstSubProperty.isPresent());
        assertEquals("Sub-property-1", firstSubProperty.get().getName());
        // Check Sub-property-2
        final Optional<Property> secondSubProperty = firstSubProperty.get().getProperty("Sub-property-2");
        assertTrue(secondSubProperty.isPresent());
        assertEquals("Sub-property-2", secondSubProperty.get().getName());
        final Optional<Property> lastSubProperty = property.getProperty("Sub-property-1", "Sub-property-2");
        assertTrue(lastSubProperty.isPresent());
        assertEquals("Sub-property-2", lastSubProperty.get().getName());
    }

    @Test
    @DisplayName("Create a property with property by the multiple path")
    void createPropertyWithPropertyByMultiplePath() {
        final Property property = new Property.Builder("Property", "Value").
                property(new String[]{"Sub-property-1", "Sub-property-2"},
                        new Property.Builder("Sub-property-3", "Sub-value-3").build()).build();
        // Check test results
        assertEquals("Property", property.getName());
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
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
    @DisplayName("Create a custom property with deleted properties")
    void createCustomPropertyWithDeletedProperties() {
        final String[] path = new String[]{"Sub-property-1", "Sub-property-2", "Sub-property-3"};
        final Property property = new Property.Builder("Property", "Value").
                property(new String[]{"Sub-property-1", "Sub-property-2"},
                        new Property.Builder("Sub-property-3", "Sub-value-3").build()).build();
        // Check test results
        assertTrue(property.getProperty(path).isPresent());
        final Property updatedProperty = new Property.Builder(property).deleteProperty(path).build();
        // Check test results
        assertFalse(updatedProperty.getProperty(path).isPresent());
    }

    @Test
    @DisplayName("Create a custom property with updated properties")
    void createCustomPropertyWithUpdatedProperties() {
        final int count = 10;
        final String[] path = new String[]{"Property-0"};
        final Property property = new Property.Builder("Property", "Value").
                properties(getProperties(0, count)).build();
        // Check test results
        assertTrue(property.getProperty(path).isPresent());
        assertEquals(count, property.getProperties().count());
        final Property updatedProperty = new Property.Builder(property).deleteProperty(path).build();
        // Check test results
        assertFalse(updatedProperty.getProperty(path).isPresent());
        assertEquals(count - 1, updatedProperty.getProperties().count());
    }

    @Test
    @DisplayName("Create a custom property")
    void createCustomProperty() {
        final Property property =
                new Property.Builder("Property", Property.Type.STRING.name(), "Value").build();
        // Check test results
        assertEquals(Property.Type.STRING, property.getType());
        assertEquals("Value", property.getValue());
    }

    @Test
    @DisplayName("Create a bool property")
    void createBoolProperty() {
        final Property property = new Property.Builder("Property", true).build();
        // Check test results
        assertEquals(Property.Type.BOOL, property.getType());
        assertTrue(property.asBool());
    }

    @Test
    @DisplayName("Create a double property")
    void createDoubleProperty() {
        final Property property = new Property.Builder("Property", 0.0).build();
        // Check test results
        assertEquals(Property.Type.DOUBLE, property.getType());
        assertEquals(0.0, property.asDouble());
    }

    @Test
    @DisplayName("Create a long property")
    void createLongProperty() {
        final Property property = new Property.Builder("Property", 0L).build();
        // Check test results
        assertEquals(Property.Type.LONG, property.getType());
        assertEquals(0L, property.asLong());
    }

    @Test
    @DisplayName("Create a array property")
    void createArrayProperty() {
        final Property property = new Property.Builder("Property", new String[]{"Value"}).build();
        // Check test results
        assertEquals(Property.Type.STRING_ARRAY, property.getType());
        assertEquals(new String[]{"Value"}[0], property.asArray()[0]);
    }

    @Test
    @DisplayName("Compare a wrong property")
    void compareWrongProperty() {
        // Check test results
        assertNotEquals(getProperty(), getConfig(Collections.emptyList()));
    }

    @Test
    @DisplayName("Compare a null property")
    void compareNullProperty() {
        // Check test results
        assertNotEquals(getProperty(), null);
    }

    @Test
    @DisplayName("Compare a property")
    void compareProperty() {
        final Property firstProperty = getProperty();
        // Check test results
        assertEquals(firstProperty, firstProperty);
    }

    @Test
    @DisplayName("Compare two simple properties")
    void compareTwoSimpleProperties() {
        final Property firstProperty = getProperty();
        final Property secondProperty = getProperty();
        // Check test results
        assertEquals(firstProperty, secondProperty);
    }

    @Test
    @DisplayName("Compare two properties")
    void compareTwoProperties() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key_1", "value_1");
        attributes.put("key_2", "value_2");
        final Property firstProperty = new Property.Builder("Property-1", "Value-1").
                caption("Caption").
                description("Description").
                attribute("key_1", "value_1").
                attributes(attributes).
                property(new String[0], new Property.Builder("Sub-property-1", "Sub-value-1").build()).
                build();
        final Property secondProperty = new Property.Builder("Property-1", "Value-1").
                caption("Caption").
                description("Description").
                attribute("key_1", "value_1").
                attributes(attributes).
                property(new String[0], new Property.Builder("Sub-property-1", "Sub-value-1").build()).
                build();
        // Check test results
        assertEquals(firstProperty, secondProperty);
    }

    @Test
    @DisplayName("Check hash codes of two properties")
    void checkHashCodesOfTwoProperties() {
        final Property firstProperty = new Property.Builder("Property", new String[]{"Value"}).build();
        final Property secondProperty = new Property.Builder("Property", new String[]{"Value"}).build();
        // Check test results
        assertEquals(firstProperty.hashCode(), secondProperty.hashCode());
    }

    @Test
    @DisplayName("Check toString() of two properties")
    void checkToStringOfTwoProperties() {
        final Property firstProperty = new Property.Builder("Property", new String[]{"Value"}).build();
        final Property secondProperty = new Property.Builder("Property", new String[]{"Value"}).build();
        // Check test results
        assertEquals(firstProperty.toString(), secondProperty.toString());
    }

    @Test
    @DisplayName("Create a property via the builder")
    void createPropertyViaBuilder() {
        final Property firstProperty = getProperty();
        final Property secondProperty = new Property.Builder(firstProperty).build();
        // Check test results
        assertEquals(firstProperty, secondProperty);
    }

    @Test
    @DisplayName("Create a property via the json builder")
    void createPropertyViaJsonBuilder() throws JsonException {
        final String json = "{\"name\":\"Property\",\"caption\":\"Caption\",\"description\":\"Description\"," +
                "\"type\":\"STRING\",\"value\":\"Value\"}";
        final Property firstProperty = new Property.Builder((JsonObject) Jsoner.deserialize(json)).build();
        // Check test results
        assertTrue(firstProperty.getAttributes().isPresent());
        assertEquals(0, firstProperty.getProperties().count());
    }

    @Test
    @DisplayName("Create a property with params via the json builder")
    void createPropertyWithParamsViaJsonBuilder() throws JsonException {
        final Property firstProperty = getProperty();
        final Property secondProperty =
                new Property.Builder((JsonObject) Jsoner.deserialize(firstProperty.toJson())).build();
        // Check test results
        assertEquals(firstProperty, secondProperty);
    }

    @Test
    @DisplayName("Convert a property to a json")
    void convertPropertyToJson() throws IOException {
        final Property property = getProperty();
        final StringWriter writer = new StringWriter();
        property.toJson(writer);
        // Check test results
        assertEquals(writer.toString(), property.toJson());
    }
}
