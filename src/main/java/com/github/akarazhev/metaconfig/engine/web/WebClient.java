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
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_NOT_PRESENTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.REQUEST_SEND_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;

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
     * Settings constants for the web client.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        public static final String CONFIG_NAME = "web-client";
        // The URL key
        public static final String URL = "url";
        // The accept all hosts key
        public static final String ACCEPT_ALL_HOSTS = "accept-all-hosts";
        // The method key
        public static final String METHOD = "method";
        // The accept key
        public static final String ACCEPT = "accept";
        // The content type key
        public static final String CONTENT_TYPE = "content-type";
        // The content key
        public static final String CONTENT = "content";
    }

    // Status code
    private int statusCode;
    // Content
    private String content;

    private WebClient(final Builder builder) {
        final Config config = builder.config;
        try {
            final Optional<Property> urlProperty = config.getProperty(Settings.URL);
            if (urlProperty.isPresent()) {
                // Accept all hosts
                final List<Throwable> exceptions = new ArrayList<>(1);
                config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(prop -> {
                            if (prop.asBool()) {
                                try {
                                    acceptAllHosts();
                                } catch (final Exception e) {
                                    exceptions.add(e);
                                }
                            }
                        }
                );
                if (exceptions.size() > 0) {
                    throw new Exception(exceptions.get(0));
                }
                // Open a connection
                final HttpsURLConnection connection =
                        (HttpsURLConnection) new URL(urlProperty.get().getValue()).openConnection();
                final Optional<Property> methodProperty = config.getProperty(METHOD);
                if (methodProperty.isPresent()) {
                    // Set a method
                    connection.setRequestMethod(methodProperty.get().getValue());
                }
                // Set the accept header
                config.getProperty(ACCEPT).ifPresent(acceptProp ->
                        connection.setRequestProperty("Accept", acceptProp.getValue()));
                // Set the content type
                config.getProperty(CONTENT_TYPE).ifPresent(contentTypeProp ->
                        connection.setRequestProperty("Content-Type", contentTypeProp.getValue()));
                final Optional<Property> contentProperty = config.getProperty(CONTENT);
                if (contentProperty.isPresent()) {
                    // Enable the output stream
                    connection.setDoOutput(true);
                    // Write the content type
                    writeContent(connection, contentProperty.get().getValue());
                }
                // Get a response code
                statusCode = connection.getResponseCode();
                // Get a content
                if (statusCode > 299) {
                    content = readContent(connection.getErrorStream());
                } else {
                    content = readContent(connection.getInputStream());
                }
                // Close the connection
                connection.disconnect();
            }
        } catch (final Exception e) {
            throw new RuntimeException(REQUEST_SEND_ERROR, e);
        }
    }

    /**
     * Returns a status response code.
     *
     * @return a status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns a content of the response.
     *
     * @return the content.
     */
    private String getContent() {
        return content;
    }

    /**
     * Returns a json content of the response.
     *
     * @return the content.
     * @throws JsonException when a web client encounters a problem.
     */
    public JsonObject getJsonContent() throws JsonException {
        return (JsonObject) Jsoner.deserialize(getContent());
    }

    private String readContent(final InputStream inputStream) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            final StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                content.append(inputLine);
            }

            return content.toString();
        }
    }

    private void writeContent(final HttpsURLConnection connection, final String content) throws IOException {
        try (final OutputStream outputStream = connection.getOutputStream()) {
            final byte[] input = content.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }
    }

    private void acceptAllHosts() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        // Empty implementation
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Empty implementation
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Empty implementation
                    }
                }
        }, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    /**
     * Wraps and builds the instance of the web client.
     */
    public final static class Builder {
        private final Config config;

        /**
         * Constructs the a client based on the configuration.
         *
         * @param config a configuration of a web client.
         */
        public Builder(final Config config) {
            // Validate the config
            this.config = Validator.of(config).
                    validate(c -> CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                    validate(c -> c.getProperty(METHOD).isPresent(), String.format(PARAM_NOT_PRESENTED, METHOD)).
                    validate(c -> c.getProperty(Settings.URL).isPresent(), String.format(PARAM_NOT_PRESENTED,
                            Settings.URL)).get();
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
