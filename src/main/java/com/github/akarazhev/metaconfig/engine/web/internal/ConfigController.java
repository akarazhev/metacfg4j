package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.util.Collections;

final class ConfigController extends AbstractController {

    private final ConfigService configService;

    ConfigController(final ConfigService configService) {
        this.configService = configService;
    }

    @Override
    void execute(final HttpExchange httpExchange) throws Exception {
        if ("GET".equals(httpExchange.getRequestMethod())) {
            JsonObject json = new JsonObject();
            json.put("status", "ok");
            String response = json.toJson();

            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            httpExchange.getRequestHeaders().put("Content-Type", Collections.singletonList("application/json"));
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.flush();
        } else {
            httpExchange.sendResponseHeaders(405, -1);
        }

        httpExchange.close();
    }
}
