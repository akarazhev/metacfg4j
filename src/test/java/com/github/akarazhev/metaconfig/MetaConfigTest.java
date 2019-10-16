package com.github.akarazhev.metaconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaConfigTest {

    @Test
    @DisplayName("Create simple meta config")
    void createMetaConfig() {
        MetaConfig metaConfig = new MetaConfig.Builder("Meta Config", Collections.emptyList()).build();
        assertEquals("Meta Config", metaConfig.getName(), "The name must be: 'Meta Config'");
    }
}
