package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.API.CONFIG_SECTION;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.DELETE;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.PUT;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionController extends AbstractController {

    ConfigSectionController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) {
        final String method = httpExchange.getRequestMethod();
        if (GET.equals(method)) {
            final OperationResponse response = configService.get(getPathParam(httpExchange.getRequestURI(), CONFIG_SECTION)).
                    map(config -> new OperationResponse.Builder<>().result(config).build()).
                    orElseGet(() -> new OperationResponse.Builder<>().error(false, "Section not found").build());
            writeResponse(httpExchange, response);
        } else if (PUT.equals(method)) {
            // todo
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
                final Config config = new Config.Builder((JsonObject) Jsoner.deserialize(bufferedReader)).build();
                writeResponse(httpExchange,
                        new OperationResponse.Builder<>().result(configService.update(config, true)).build());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // todo
        } else if (DELETE.equals(method)) {
            configService.remove(getPathParam(httpExchange.getRequestURI(), CONFIG_SECTION));
            writeResponse(httpExchange, new OperationResponse.Builder<>().result(Boolean.TRUE).build());
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }
}
