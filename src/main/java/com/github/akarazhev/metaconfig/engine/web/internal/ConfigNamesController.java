package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.util.stream.Collectors;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigNamesController extends AbstractController {

    private ConfigNamesController(final Builder builder) {
        super(builder);
    }

    @Override
    void execute(final HttpExchange httpExchange) {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response =
                    new OperationResponse.Builder<>().result(configService.getNames().collect(Collectors.toList())).build();
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    static class Builder extends AbstractBuilder {

        Builder(final ConfigService configService) {
            super(configService);
        }

        ConfigNamesController build() {
            return new ConfigNamesController(this);
        }
    }
}
