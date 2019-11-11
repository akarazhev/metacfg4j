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
package com.github.akarazhev.metaconfig.engine.web;

/**
 * Constants of the web client-server.
 */
public final class Constants {

    private Constants() {
        throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
    }

    private static final String CREATE_CONSTANT_CLASS_ERROR = "Constant class can not be created.";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String ACCEPT = "Accept";

    /**
     * Method constants for the web client-server.
     */
    public final static class Method {

        private Method() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        public static final String POST = "POST";
        public static final String DELETE = "DELETE";
        public static final String GET = "GET";
        public static final String PUT = "PUT";
    }

    /**
     * API constants for controllers.
     */
    public final static class API {

        private API() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        public static final String ACCEPT_CONFIG = "/api/metacfg/accept_config";
        public static final String CONFIG_NAMES = "/api/metacfg/config_names";
        public static final String CONFIG = "/api/metacfg/config";
    }
}
