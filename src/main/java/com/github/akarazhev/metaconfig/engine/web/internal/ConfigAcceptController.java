package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_ACCEPT;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.POST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigAcceptController extends AbstractController {

    ConfigAcceptController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) {
        if (POST.equals(httpExchange.getRequestMethod())) {
            final String name = getPathParam(httpExchange.getRequestURI(), CONFIG_ACCEPT);
            configService.accept(name);
            writeResponse(httpExchange, new OperationResponse.Builder<>().result("Accepted '" + name + "' config").build());
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }
}
