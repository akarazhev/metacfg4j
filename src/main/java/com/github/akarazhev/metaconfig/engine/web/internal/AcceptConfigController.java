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

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.POST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class AcceptConfigController extends AbstractController {

    private AcceptConfigController(final Builder builder) {
        super(builder);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (POST.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI(), ACCEPT_CONFIG).findAny().
                    map(param -> {
                        configService.accept(param);
                        return new OperationResponse.Builder<>().result("Accepted '" + param + "' config").build();
                    }).
                    orElseGet(() -> new OperationResponse.Builder<>().error("Path param is not present").build());
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    static class Builder extends AbstractBuilder {

        Builder(final ConfigService configService) {
            super(configService);
        }

        AcceptConfigController build() {
            return new AcceptConfigController(this);
        }
    }
}
