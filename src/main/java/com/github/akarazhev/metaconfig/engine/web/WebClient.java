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

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.Property;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * The internal implementation of the web client. The config name must be "web-client".
 * The following parameters can be set in a config property (e.g. name, value):
 * - url: the target URL;
 * - method: http method;
 * - accept: the accept header;
 * - content-type: a content type;
 * - content: a content;
 */
public final class WebClient {
    /**
     * Constants for the web client.
     */
    final static class Constants {
        // The configuration name
        static final String CONFIG_NAME = "web-client";
        // The URL key
        static final String URL = "url";
        // The method key
        static final String METHOD = "method";
        // The accept key
        static final String ACCEPT = "accept";
        // The content type key
        static final String CONTENT_TYPE = "content-type";
        // The content key
        static final String CONTENT = "content";
    }

    // Status code
    private int statusCode;
    // Content
    private String content;

    private WebClient(final Builder builder) {
        final Config config = builder.config;
        try {
            Optional<Property> property = config.getProperty(Constants.URL);
            if (property.isPresent()) {
                // Open a connection
                final HttpURLConnection connection = (HttpURLConnection) new URL(property.get().getValue()).openConnection();
                property = config.getProperty(Constants.METHOD);
                if (property.isPresent()) {
                    // Set a method
                    connection.setRequestMethod(property.get().getValue());
                }
                property = config.getProperty(Constants.ACCEPT);
                if (property.isPresent()) {
                    // Set the accept header
                    connection.setRequestProperty(WebConstants.ACCEPT, property.get().getValue());
                }
                property = config.getProperty(Constants.CONTENT_TYPE);
                if (property.isPresent()) {
                    // Set the content type
                    connection.setRequestProperty(WebConstants.CONTENT_TYPE, property.get().getValue());
                }
                property = config.getProperty(Constants.CONTENT);
                if (property.isPresent()) {
                    // Enable the output stream
                    connection.setDoOutput(true);
                    // Set the content type
                    setContent(property.get().getValue(), connection);
                }
                // Get a response code
                statusCode = connection.getResponseCode();
                // Get a content
                if (statusCode > 299) {
                    content = getContent(connection.getErrorStream());
                } else {
                    content = getContent(connection.getInputStream());
                }
                // Close the connection
                connection.disconnect();
            }
        } catch (Exception e) {
            throw new RuntimeException("Request can not be performed", e);
        }
    }

    /**
     * Returns a status response code.
     *
     * @return a status code.
     */
    int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns a json content of the response.
     *
     * @return the content.
     * @throws JsonException when a web client encounters a problem.
     */
    JsonObject getJsonContent() throws JsonException {
        return (JsonObject) Jsoner.deserialize(content);
    }

    private String getContent(final InputStream inputStream) throws IOException {
        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            final StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                content.append(inputLine);
            }

            return content.toString();
        }
    }

    private void setContent(final String content, final HttpURLConnection connection) throws IOException {
        try (final OutputStream os = connection.getOutputStream()) {
            final byte[] input = content.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    /**
     * Wraps and builds the instance of the web client.
     */
    public final static class Builder {
        private Config config;

        /**
         * Constructs the web client based on the configuration.
         *
         * @param config a configuration of a web client.
         */
        public Builder(final Config config) {
            this.config = Objects.requireNonNull(config);
            if (!Constants.CONFIG_NAME.equals(this.config.getName())) {
                throw new RuntimeException("Configuration name is wrong");
            }
        }

        /**
         * Builds the web client with parameters.
         *
         * @return a builder of the web client.
         */
        public WebClient build() {
            return new WebClient(this);
        }
    }
}
