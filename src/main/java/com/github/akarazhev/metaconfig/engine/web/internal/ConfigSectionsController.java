package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionsController extends AbstractController {

    private ConfigSectionsController(final Builder builder) {
        super(builder);
    }

    @Override
    void execute(final HttpExchange httpExchange) throws IOException {
        if (GET.equals(httpExchange.getRequestMethod())) {
            final OperationResponse response = getRequestParam(httpExchange.getRequestURI().getQuery(), "names").
                    map(param -> {
                        try {
                            final String array = new String(Base64.getDecoder().decode(param), StandardCharsets.UTF_8);
                            final JsonArray jsonArray = (JsonArray) Jsoner.deserialize(array);
                            final List<Config> sections = new ArrayList<>(jsonArray.size());
                            for (int i = 0; i < jsonArray.size(); i++) {
                                configService.get(jsonArray.getString(i)).ifPresent(sections::add);
                            }

                            return new OperationResponse.Builder<>().result(sections).build();
                        } catch (Exception e) {
                            return new OperationResponse.Builder<>().error("Request param can not be parsed").build();
                        }
                    }).
                    orElseGet(() -> new OperationResponse.Builder<>().error("Request param is not present").build());
            writeResponse(httpExchange, response);
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }

    static class Builder extends AbstractBuilder {

        Builder(final ConfigService configService) {
            super(configService);
        }

        ConfigSectionsController build() {
            return new ConfigSectionsController(this);
        }
    }
}
