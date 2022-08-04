/* Copyright 2019-2022 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;

/**
 * Constants of the web client-server.
 */
public final class Constants {

    private Constants() {
        throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
    }

    /**
     * Header constants for the web client-server.
     */
    public final static class Header {

        private Header() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        public static final String APPLICATION_JSON = "application/json";
    }

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
}
