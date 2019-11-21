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

import com.github.akarazhev.metaconfig.Constants;
import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.JSON_TO_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.REQUEST_PARAM_NOT_PRESENT;
import static com.github.akarazhev.metaconfig.Constants.Messages.STRING_TO_JSON_ERROR;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.PUT;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

/**
 * Provides a handler functionality for the GET, PUT, DELETE config methods.
 */
final class ConfigController extends AbstractController {

    private ConfigController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        final URI uri = httpExchange.getRequestURI();
        final String method = httpExchange.getRequestMethod();
        final Supplier<OperationResponse> requestParamNotPresent =
                () -> new OperationResponse.Builder<>().error(REQUEST_PARAM_NOT_PRESENT).build();
        if (GET.equals(method)) {
            final OperationResponse response = getRequestParam(uri.getQuery(), REQ_PARAM_NAMES).
                    map(param -> {
                        try {
                            final Collection<Config> configs =
                                    configService.get(getValues(param)).collect(Collectors.toList());
                            return new OperationResponse.Builder<>().result(configs).build();
                        } catch (final Exception e) {
                            return new OperationResponse.Builder<>().error(STRING_TO_JSON_ERROR).build();
                        }
                    }).
                    orElseGet(() -> {
                        final Collection<Config> configs = configService.get().collect(Collectors.toList());
                        return new OperationResponse.Builder<>().result(configs).build();
                    });
            writeResponse(httpExchange, response);
        } else if (PUT.equals(method)) {
            try (final BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                final JsonArray jsonConfigs = (JsonArray) Jsoner.deserialize(bufferedReader);
                final Stream<Config> stream = jsonConfigs.stream().
                        map(config -> new Config.Builder((JsonObject) config).build());
                final Collection<Config> updatedConfigs = configService.update(stream).
                        collect(Collectors.toList());
                writeResponse(httpExchange, new OperationResponse.Builder<>().result(updatedConfigs).build());
            } catch (final JsonException e) {
                throw new InvalidRequestException(BAD_REQUEST.getCode(), JSON_TO_CONFIG_ERROR);
            }
        } else if (DELETE.equals(method)) {
            final OperationResponse response = getPathParams(uri.getPath(), CONFIG).findAny().
                    map(param -> {
                        try {
                            final int count = configService.remove(getValues(param));
                            return new OperationResponse.Builder<>().result(count).build();
                        } catch (final Exception e) {
                            return new OperationResponse.Builder<>().error(STRING_TO_JSON_ERROR).build();
                        }
                    }).
                    orElseGet(requestParamNotPresent);
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), Constants.Messages.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Wraps and builds the instance of the config controller.
     */
    static class Builder extends AbstractBuilder {
        /**
         * Constructs a controller with the configuration service param.
         *
         * @param configService a configuration service.
         */
        Builder(final ConfigService configService) {
            super(configService);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        ConfigController build() {
            return new ConfigController(this);
        }
    }
}
