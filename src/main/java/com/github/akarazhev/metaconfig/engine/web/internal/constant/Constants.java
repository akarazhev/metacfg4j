package com.github.akarazhev.metaconfig.engine.web.internal.constant;

public final class Constants {

    private Constants() {
        // Constants class
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public final static class Method {

        private Method() {
            // Constants class
        }

        public static final String POST = "POST";
        public static final String GET = "GET";
        public static final String PUT = "PUT";
    }

    public final static class API {
        private API() {
            // Constants class
        }

        public static final String PING = "/api/config/ping";
    }
}
