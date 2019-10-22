package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.util.Collections;

import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.METHOD_NOT_ALLOWED;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.OK;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.Constants.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.Constants.CONTENT_TYPE;

final class ConfigController extends AbstractController {

    private final ConfigService configService;

    ConfigController(final ConfigService configService) {
        this.configService = configService;
    }

    @Override
    void execute(final HttpExchange httpExchange) throws Exception {
        if ("GET".equals(httpExchange.getRequestMethod())) {
            final JsonObject json = new JsonObject();
            json.put("status", "ok");
            final String response = json.toJson();

            httpExchange.sendResponseHeaders(OK.getCode(), response.getBytes().length);
            httpExchange.getRequestHeaders().put(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON));
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.flush();
        } else {
            httpExchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
        }

        httpExchange.close();
    }
}
