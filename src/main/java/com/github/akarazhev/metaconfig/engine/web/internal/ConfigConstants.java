package com.github.akarazhev.metaconfig.engine.web.internal;

final class ConfigConstants {

    private ConfigConstants() {
        // ConfigConstants class
    }

    static final String CONTENT_TYPE = "Content-Type";
    static final String APPLICATION_JSON = "application/json";

    final static class Method {

        private Method() {
            // ConfigConstants class
        }

        static final String POST = "POST";
        static final String DELETE = "DELETE";
        static final String GET = "GET";
        static final String PUT = "PUT";
    }

    final static class API {
        private API() {
            // ConfigConstants class
        }

        static final String ACCEPT_CONFIG = "/api/config/accept";
        static final String CONFIG_NAMES = "/api/config/names";
        static final String CONFIG_SECTIONS = "/api/config/sections";
        static final String CONFIG_SECTION = "/api/config/section";
    }
}
