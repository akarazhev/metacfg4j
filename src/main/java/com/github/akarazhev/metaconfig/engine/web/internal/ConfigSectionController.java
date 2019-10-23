package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionController extends AbstractController {

    ConfigSectionController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws Exception {
        final URI requestURI = httpExchange.getRequestURI();
        if (GET.equals(httpExchange.getRequestMethod())) {
            final String name = requestURI.getPath().substring(ConfigConstants.API.CONFIG_SECTION.length());
            if (name.length() == 0) {
                throw new InvalidRequestException(BAD_REQUEST.getCode(), "Section name is empty");
            }

            final OperationResponse response = configService.get(name).
                    map(config -> new OperationResponse.Builder<>().result(config).build()).
                    orElseGet(() -> new OperationResponse.Builder<>().error(false, "Section not found").build());
            writeResponse(httpExchange, response);
        } else {
            httpExchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
        }
    }
}
