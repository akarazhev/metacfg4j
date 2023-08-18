/* Copyright 2019-2023 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web.server;

import com.github.akarazhev.metaconfig.Constants;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.api.PageRequest;
import com.github.akarazhev.metaconfig.api.PageResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.github.akarazhev.metaconfig.Constants.Messages.STRING_TO_JSON_ERROR;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.GET;
import static com.github.akarazhev.metaconfig.extension.WebUtils.getRequestParam;
import static com.github.akarazhev.metaconfig.extension.WebUtils.getValue;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

/**
 * Provides a handler functionality for the GET config names method.
 */
final class ConfigNamesController extends AbstractController {
    private final static Logger LOGGER = Logger.getLogger(ConfigNamesController.class.getSimpleName());

    private ConfigNamesController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        final String method = httpExchange.getRequestMethod();
        if (GET.equals(method)) {
            final URI uri = httpExchange.getRequestURI();
            final Optional<String> param = getRequestParam(uri, REQ_PARAM_PAGE_REQUEST);
            if (param.isPresent()) {
                try {
                    final PageResponse response =
                            configService.getNames(new PageRequest.Builder(getValue(param.get())).build());
                    writeResponse(httpExchange, new OperationResponse.Builder<PageResponse>().result(response).build());
                } catch (final Exception e) {
                    LOGGER.log(Level.SEVERE, e.toString());
                    writeResponse(httpExchange,
                            new OperationResponse.Builder<PageResponse>().error(STRING_TO_JSON_ERROR).build());
                }
            } else {
                final List<String> names = configService.getNames().collect(Collectors.toList());
                writeResponse(httpExchange, new OperationResponse.Builder<List<String>>().result(names).build());
            }
        } else {
            throw new MethodNotAllowedException(HTTP_BAD_METHOD, Constants.Messages.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Wraps and builds the instance of the config names controller.
     */
    final static class Builder extends AbstractBuilder {

        /**
         * Constructs a controller with the configuration service param.
         *
         * @param configService a configuration service.
         */
        Builder(final ConfigService configService) {
            super("", configService);
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
