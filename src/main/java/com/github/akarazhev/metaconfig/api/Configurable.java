package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.json_simple.ExtJsonable;

import java.util.Map;
import java.util.stream.Stream;

public interface Configurable extends ExtJsonable {

    Map<String, String> getAttributes();

    Stream<Property> getProperties();
}
