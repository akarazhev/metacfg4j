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
package com.github.akarazhev.metaconfig;

/**
 * Library constants.
 */
public final class Constants {

    public static final String CREATE_CONSTANT_CLASS_ERROR = "Constant class can not be created.";

    private Constants() {
        throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
    }

    /**
     * Messages constants for the library.
     */
    public final static class Messages {

        private Messages() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        public static final String CREATE_FACTORY_CLASS_ERROR = "Factory class can not be created.";
        public static final String CREATE_UTILS_CLASS_ERROR = "Utils class can not be created.";
        public static final String META_CONFIG_ERROR = "MetaConfig can not be instantiated.";
        public static final String STRING_TO_JSON_ERROR = "String can not be parsed to JSON.";
        public static final String WRONG_ID_VALUE = "Id value must be grater zero.";
        public static final String WRONG_VERSION_VALUE = "Version value must be grater zero.";
        public static final String WRONG_UPDATED_VALUE = "Updated value must be grater zero.";
        public static final String WRONG_CONFIG_NAME = "Config name is wrong.";
        public static final String REQUEST_SEND_ERROR = "Request can not be sent.";
        public static final String SERVER_STARTED = "Server started.";
        public static final String SERVER_STOPPED = "Server stopped.";
        public static final String METHOD_NOT_ALLOWED = "Method not allowed.";
        public static final String PATH_PARAM_NOT_PRESENT = "Path param is not present.";
        public static final String REQUEST_PARAM_NOT_PRESENT = "Request param is not present.";
        public static final String JSON_TO_CONFIG_ERROR = "JSON can not be parsed to config.";
        public static final String CONFIG_ACCEPTED = "Accepted '%s' config.";
        public static final String CREATE_CONFIG_TABLE_ERROR = "'Configs' table can not be created.";
        public static final String INSERT_CONFIGS_ERROR = "Configs instances can not be inserted.";
        public static final String INSERT_ATTRIBUTES_ERROR = "Attributes can not be inserted.";
        public static final String UPDATE_ATTRIBUTES_ERROR = "Attributes can not be updated.";
        public static final String INSERT_CONFIG_PROPERTIES_ERROR = "Config properties can not be inserted.";
        public static final String UPDATE_CONFIGS_ERROR = "Configs instances can not be updated.";
        public static final String RECEIVED_CONFIGS_ERROR = "Config instances can not be received.";
        public static final String DELETE_CONFIGS_ERROR = "Configs can not be deleted.";
        public static final String DB_ERROR = "Database error.";
        public static final String DB_ROLLBACK_ERROR = "Database rollback error.";
        public static final String DB_CONNECTION_ERROR = "Database connection error.";
    }
}
