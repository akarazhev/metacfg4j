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
import com.github.akarazhev.metaconfig.extension.URLUtils;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.CONFIG_ACCEPT_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.DELETE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.RECEIVED_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SAVE_CONFIGS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_WRONG_STATUS_CODE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Header.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.POST;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.PUT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.ACCEPT_ALL_HOSTS;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.METHOD;
import static com.github.akarazhev.metaconfig.engine.web.WebClient.Settings.URL;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.ERROR;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.RESULT;
import static com.github.akarazhev.metaconfig.engine.web.server.OperationResponse.Fields.SUCCESS;
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
    public Stream<Config> findByNames(final Stream<String> stream) {
        return ((JsonArray) getContent(getProperties(stream, GET), RECEIVED_CONFIGS_ERROR)).stream().
                map(config -> new Config.Builder((JsonObject) config).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> findNames() {
        final Collection<Property> properties = new ArrayList<>(3);
        this.config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(property ->
                properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, property.asBool()).build()));
        this.config.getProperty(URL).ifPresent(property ->
                properties.add(new Property.Builder(URL, property.getValue() + "/config_names").build()));
        properties.add(new Property.Builder(METHOD, GET).build());

        return ((JsonArray) getContent(properties, RECEIVED_CONFIGS_ERROR)).stream().map(Objects::toString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Config> saveAndFlush(final Stream<Config> stream) {
        final Collection<Property> properties = new ArrayList<>(6);
        this.config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(property ->
                properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, property.asBool()).build()));
        this.config.getProperty(URL).ifPresent(property ->
                properties.add(new Property.Builder(URL, property.getValue() + "/config").build()));
        properties.add(new Property.Builder(METHOD, PUT).build());
        properties.add(new Property.Builder(ACCEPT, APPLICATION_JSON).build());
        properties.add(new Property.Builder(CONTENT_TYPE, APPLICATION_JSON).build());
        properties.add(new Property.Builder(CONTENT, Jsoner.serialize(stream.toArray(Config[]::new))).build());

        return ((JsonArray) getContent(properties, SAVE_CONFIGS_ERROR)).stream().
                map(config -> new Config.Builder((JsonObject) config).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(final Stream<String> stream) {
        return ((BigDecimal) getContent(getProperties(stream, DELETE), DELETE_CONFIGS_ERROR)).intValue();
    }

    /**
     * Accepts a configuration model by the name.
     *
     * @param name a configuration name.
     */
    public void accept(final String name) {
        final Collection<Property> properties = new ArrayList<>(3);
        this.config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(property ->
                properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, property.asBool()).build()));
        this.config.getProperty(URL).ifPresent(property ->
                properties.add(new Property.Builder(URL, property.getValue() + "/accept_config/" +
                        URLUtils.encode(name)).build()));
        properties.add(new Property.Builder(METHOD, POST).build());

        getContent(properties, CONFIG_ACCEPT_ERROR);
    }

    private Collection<Property> getProperties(final Stream<String> stream, final String method) {
        final Collection<Property> properties = new ArrayList<>(3);
        this.config.getProperty(ACCEPT_ALL_HOSTS).ifPresent(property ->
                properties.add(new Property.Builder(ACCEPT_ALL_HOSTS, property.asBool()).build()));
        this.config.getProperty(URL).ifPresent(property ->
                properties.add(new Property.Builder(URL, property.getValue() + "/configs?names=" +
                        getNames(stream)).build()));
        properties.add(new Property.Builder(METHOD, method).build());
        return properties;
    }

    private String getNames(final Stream<String> stream) {
        final String jsonNames = new JsonArray(Arrays.asList(stream.toArray(String[]::new))).toJson();
        return new String(Base64.getEncoder().encode(jsonNames.getBytes()), StandardCharsets.UTF_8);
    }

    private Object getContent(final Collection<Property> properties, final String error) {
        try {
            final WebClient client = new WebClient.Builder(new Config.Builder(CONFIG_NAME, properties).build()).build();
            final int code = client.getStatusCode();
            if (code == HTTP_OK) {
                final JsonObject content = client.getJsonContent();
                if ((Boolean) content.get(SUCCESS)) {
                    return content.get(RESULT);
                } else {
                    throw new IOException((String) content.get(ERROR));
                }
            } else {
                throw new IOException(String.format(SERVER_WRONG_STATUS_CODE, code));
            }
        } catch (Exception e) {
            throw new RuntimeException(error, e);
        }
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
