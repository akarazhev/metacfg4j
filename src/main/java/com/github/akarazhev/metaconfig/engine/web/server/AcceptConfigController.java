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
package com.github.akarazhev.metaconfig.engine.web.server;

import com.github.akarazhev.metaconfig.Constants;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

import static com.github.akarazhev.metaconfig.Constants.Messages.CONFIG_ACCEPTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.PATH_PARAM_NOT_PRESENT;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.Method.POST;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

/**
 * Provides a handler functionality for the POST accept method.
 */
final class AcceptConfigController extends AbstractController {

    private AcceptConfigController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (POST.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI().getPath(), apiPath +
                    ACCEPT_CONFIG).findAny().
                    map(param -> {
                        configService.accept(param);
                        return new OperationResponse.Builder<>().result(String.format(CONFIG_ACCEPTED, param)).build();
                    }).
                    orElseGet(() -> new OperationResponse.Builder<>().error(PATH_PARAM_NOT_PRESENT).build());
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
