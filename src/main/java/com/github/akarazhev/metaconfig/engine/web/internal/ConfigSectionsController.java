package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.Method.GET;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.METHOD_NOT_ALLOWED;

final class ConfigSectionsController extends AbstractController {

    ConfigSectionsController(final ConfigService configService) {
        super(configService);
    }

    @Override
    void execute(final HttpExchange httpExchange) {
        if (GET.equals(httpExchange.getRequestMethod())) {
            try {
                final String param = getRequestParam(httpExchange.getRequestURI().getQuery(), "names");
                final JsonArray jsonArray = (JsonArray) Jsoner.deserialize(new String(Base64.getDecoder().decode(param)));
                final List<Config> sections = new ArrayList<>(jsonArray.size());
                for (int i = 0; i < jsonArray.size(); i++) {
                    configService.get(jsonArray.getString(i)).ifPresent(sections::add);
                }

                writeResponse(httpExchange, new OperationResponse.Builder<>().result(sections).build());
            } catch (Exception e) {
                throw new InvalidRequestException(BAD_REQUEST.getCode(), "Bad request");
            }
        } else {
            throw new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), "Method not allowed");
        }
    }
}
