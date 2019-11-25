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
package com.github.akarazhev.metaconfig.api;

import com.github.akarazhev.metaconfig.engine.web.WebClient;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_WRONG_STATUS_CODE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * {@inheritDoc}
 */
final class WebConfigRepository implements ConfigRepository {
    private final Config config;

    private WebConfigRepository(final Builder builder) {
        this.config = builder.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> findByNames(Stream<String> stream) {
        return null; // todo
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        final Collection<Property> properties = new ArrayList<>(3);
        properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, true).build());

        this.config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(property ->
                properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, property.asBool()).build()));

        this.config.getProperty(URL).ifPresent(property ->
                properties.add(new Property.Builder(URL, property.getValue() + "/config_names").build()));

        properties.add(new Property.Builder(METHOD, GET).build());
        final Config config = new Config.Builder(CONFIG_NAME, properties).build();

        try {
            final WebClient client = new WebClient.Builder(config).build();
            final int code = client.getStatusCode();
            if (code == HTTP_OK) {
                final JsonObject content = client.getJsonContent();
                if ((Boolean) content.get("success")) {
                    return ((JsonArray) content.get("result")).stream().map(Objects::toString);
                }
            } else {
                throw new Exception(String.format(SERVER_WRONG_STATUS_CODE, code));
            }
        } catch (Exception e) {
            throw new RuntimeException(RECEIVED_CONFIGS_ERROR, e);
        }

        return Stream.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> saveAndFlush(Stream<Config> stream) {
        return null; // todo
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Stream<String> stream) {
        return 0; // todo
    }

    /**
     * Wraps and builds the instance of the web config repository.
     */
    public final static class Builder {
        private final Config config;

        /**
         * Constructs a web config repository with a required parameter.
         *
         * @param config the datasource.
         */
        Builder(final Config config) {
            this.config = Validator.of(config).get();
        }

        /**
         * Builds a web config repository with a required parameter.
         *
         * @return a builder of the web config repository.
         */
        public ConfigRepository build() {
            return new WebConfigRepository(this);
        }
    }
}
