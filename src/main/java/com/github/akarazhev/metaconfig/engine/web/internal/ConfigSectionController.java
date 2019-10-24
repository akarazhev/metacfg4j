package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_SECTION;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.PUT;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionController extends AbstractController {

    private ConfigSectionController(final Builder builder) {
        super(builder);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        final String method = httpExchange.getRequestMethod();
        final Supplier<OperationResponse> paramIsNotPresent =
                () -> new OperationResponse.Builder<>().error("Path param is not present").build();
        if (GET.equals(method)) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI(), CONFIG_SECTION).findAny().
                    map(param -> configService.get(param).
                            map(config -> new OperationResponse.Builder<>().result(config).build()).
                            orElseGet(() -> new OperationResponse.Builder<>().error("Section not found").build())
                    ).
                    orElseGet(paramIsNotPresent);
            writeResponse(httpExchange, response);
        } else if (PUT.equals(method)) {
            // it can be re-implemented
            try (final BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                final Config config = new Config.Builder((JsonObject) Jsoner.deserialize(bufferedReader)).build();
                writeResponse(httpExchange,
                        new OperationResponse.Builder<>().result(configService.update(config, true)).build());
            } catch (JsonException e) {
                throw new InvalidRequestException(BAD_REQUEST.getCode(), "Config can not be parsed");
            }
        } else if (DELETE.equals(method)) {
            final OperationResponse response = getPathParams(httpExchange.getRequestURI(), CONFIG_SECTION).findAny().
                    map(param -> {
                        configService.remove(param);
                        return new OperationResponse.Builder<>().result(true).build();
                    }).
                    orElseGet(paramIsNotPresent);
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    static class Builder extends AbstractBuilder {

        Builder(final ConfigService configService) {
            super(configService);
        }

        ConfigSectionController build() {
            return new ConfigSectionController(this);
        }
    }
}
