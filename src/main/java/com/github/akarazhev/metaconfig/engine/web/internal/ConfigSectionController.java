package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_SECTION;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionController extends AbstractController {

    ConfigSectionController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws Exception {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response = configService.get(getPathParam(httpExchange.getRequestURI(), CONFIG_SECTION)).
                    map(config -> new OperationResponse.Builder<>().result(config).build()).
                    orElseGet(() -> new OperationResponse.Builder<>().error(false, "Section not found").build());
            writeResponse(httpExchange, response);
        } else {
            httpExchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
        }
    }
}
