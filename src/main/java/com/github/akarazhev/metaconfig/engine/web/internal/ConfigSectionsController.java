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

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

/**
 * Provides a handler functionality for the GET config sections method.
 */
final class ConfigSectionsController extends AbstractController {

    private ConfigSectionsController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response = getRequestParam(httpExchange.getRequestURI().getQuery(), "names").
                    map(param -> {
                        try {
                            final String array = new String(Base64.getDecoder().decode(param), StandardCharsets.UTF_8);
                            final JsonArray jsonArray = (JsonArray) Jsoner.deserialize(array);
                            final List<Config> sections = new ArrayList<>(jsonArray.size());
                            for (int i = 0; i < jsonArray.size(); i++) {
                                configService.get(jsonArray.getString(i)).ifPresent(sections::add);
                            }

                            return new OperationResponse.Builder<>().result(sections).build();
                        } catch (Exception e) {
                            return new OperationResponse.Builder<>().error("Request param can not be parsed").build();
                        }
                    }).
                    orElseGet(() -> new OperationResponse.Builder<>().error("Request param is not present").build());
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    /**
     * Wraps and builds the instance of the config sections controller.
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
        ConfigSectionsController build() {
            return new ConfigSectionsController(this);
        }
    }
}
