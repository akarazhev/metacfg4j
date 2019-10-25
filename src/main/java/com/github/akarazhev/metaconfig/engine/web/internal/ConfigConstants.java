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
