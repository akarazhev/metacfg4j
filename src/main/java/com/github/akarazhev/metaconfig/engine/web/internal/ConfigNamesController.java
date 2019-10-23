package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.util.stream.Collectors;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigNamesController extends AbstractController {

    ConfigNamesController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws Exception {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response =
                    new OperationResponse.Builder<>().result(configService.getNames().collect(Collectors.toList())).build();
            writeResponse(httpExchange, response);
        } else {
            httpExchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
        }
    }
}
