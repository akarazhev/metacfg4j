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
