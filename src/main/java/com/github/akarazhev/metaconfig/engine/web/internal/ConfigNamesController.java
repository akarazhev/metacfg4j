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

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.stream.Collectors;

import static com.github.akarazhev.metaconfig.engine.web.WebConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

/**
 * Provides a handler functionality for the GET config names method.
 */
final class ConfigNamesController extends AbstractController {

    private ConfigNamesController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response =
                    new OperationResponse.Builder<>().result(configService.getNames().collect(Collectors.toList())).build();
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    /**
     * Wraps and builds the instance of the config names controller.
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
        ConfigNamesController build() {
            return new ConfigNamesController(this);
        }
    }
}
