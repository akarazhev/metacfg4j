package com.github.akarazhev.metaconfig.engine.web.internal;

final class ConfigConstants {

    private ConfigConstants() {
        // ConfigConstants class
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public final static class Method {

        private Method() {
            // ConfigConstants class
        }

        public static final String POST = "POST";
        public static final String GET = "GET";
        public static final String PUT = "PUT";
    }

    public final static class API {
        private API() {
            // ConfigConstants class
        }

        public static final String CONFIG_ACCEPT = "/api/config/accept";
        public static final String CONFIG_NAMES = "/api/config/names";
        public static final String CONFIG_SECTIONS = "/api/config/sections";
        public static final String CONFIG_SECTION = "/api/config/section";
    }
}
