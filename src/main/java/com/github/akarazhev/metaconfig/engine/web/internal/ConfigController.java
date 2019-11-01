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
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static com.github.akarazhev.metaconfig.Constants.Messages.CONFIG_NOT_FOUND;
import static com.github.akarazhev.metaconfig.Constants.Messages.JSON_TO_CONFIG_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PATH_PARAM_NOT_PRESENT;
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
        final String method = httpExchange.getRequestMethod();
        final Supplier<OperationResponse> paramIsNotPresent =
                () -> new OperationResponse.Builder<>().error(PATH_PARAM_NOT_PRESENT).build();
        if (GET.equals(method)) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI().getPath(), CONFIG).findAny().
                    map(param -> configService.get(param).
                            map(config -> new OperationResponse.Builder<>().result(config).build()).
                            orElseGet(() -> new OperationResponse.Builder<>().error(CONFIG_NOT_FOUND).build())
                    ).
                    orElseGet(paramIsNotPresent);
            writeResponse(httpExchange, response);
        } else if (PUT.equals(method)) {
            // it can be re-implemented
            try (final BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                final Config config = new Config.Builder((JsonObject) Jsoner.deserialize(bufferedReader)).build();
                writeResponse(httpExchange,
                        new OperationResponse.Builder<>().result(configService.update(config, true)).build());
            } catch (JsonException e) {
                throw new InvalidRequestException(BAD_REQUEST.getCode(), JSON_TO_CONFIG_ERROR);
            }
        } else if (DELETE.equals(method)) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI().getPath(), CONFIG).findAny().
                    map(param -> {
                        configService.remove(param);
                        return new OperationResponse.Builder<>().result(true).build();
                    }).
                    orElseGet(paramIsNotPresent);
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
