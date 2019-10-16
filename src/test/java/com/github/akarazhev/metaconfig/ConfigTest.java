package com.github.akarazhev.metaconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {

    @Test
    @DisplayName("Create simple meta config")
    void createMetaConfig() {
        Config config = new Config.Builder("Meta Config", Collections.emptyList()).build();
        assertEquals("Meta Config", config.getName(), "The name must be: 'Meta Config'");
    }
}
