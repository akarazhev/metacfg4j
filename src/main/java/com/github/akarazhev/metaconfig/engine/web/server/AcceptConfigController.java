/* Copyright 2019-2021 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web.server;

import com.github.akarazhev.metaconfig.Constants;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.Messages.CONFIG_ACCEPTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.PATH_PARAM_NOT_PRESENT;
import static com.github.akarazhev.metaconfig.Constants.Messages.STRING_TO_JSON_ERROR;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.POST;
import static com.github.akarazhev.metaconfig.extension.WebUtils.getPathParams;
import static com.github.akarazhev.metaconfig.extension.WebUtils.getValues;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

/**
 * Provides a handler functionality for the POST accept method.
 */
final class AcceptConfigController extends AbstractController {
    private final static Logger LOGGER = Logger.getLogger(AcceptConfigController.class.getSimpleName());

    private AcceptConfigController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (POST.equals(httpExchange.getRequestMethod())) {
            final OperationResponse<String> response = getPathParams(httpExchange.getRequestURI(), apiPath).
                    findAny().
                    map(param -> {
                        try {
                            configService.accept(getValues(param));
                            final String result = String.format(CONFIG_ACCEPTED, param);
                            return new OperationResponse.Builder<String>().result(result).build();
                        } catch (final Exception e) {
                            LOGGER.log(Level.SEVERE, e.toString());
                            return new OperationResponse.Builder<String>().error(STRING_TO_JSON_ERROR).build();
                        }
                    }).
                    orElseGet(() -> new OperationResponse.Builder<String>().error(PATH_PARAM_NOT_PRESENT).build());
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(HTTP_BAD_METHOD, Constants.Messages.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Wraps and builds the instance of the accept controller.
     */
    final static class Builder extends AbstractBuilder {

        /**
         * Constructs a controller with the configuration service param.
         *
         * @param apiPath       an api path.
         * @param configService a configuration service.
         */
        Builder(final String apiPath, final ConfigService configService) {
            super(apiPath, configService);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        AcceptConfigController build() {
            return new AcceptConfigController(this);
        }
    }
}
