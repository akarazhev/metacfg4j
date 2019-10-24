package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.OK;

abstract class AbstractController {

    final ConfigService configService;

    AbstractController(final AbstractBuilder abstractBuilder) {
        this.configService = abstractBuilder.configService;
    }

    void handle(HttpExchange httpExchange) {
        try {
            execute(httpExchange);
        } catch (Exception exception) {
            handle(exception, httpExchange);
        } finally {
            httpExchange.close();
        }
    }

    abstract void execute(final HttpExchange httpExchange) throws IOException;

    String getPathParam(final URI uri, final String api) throws IOException {
        // todo it is very simple implementation
        final String name = uri.getPath().substring(api.length());
        if (name.length() == 0) {
            throw new InvalidRequestException(BAD_REQUEST.getCode(), "Param is empty");
        }

        return name;
    }

    String getRequestParam(final String query, final String param) throws IOException {
        if (!query.contains(param)) {
            throw new InvalidRequestException(BAD_REQUEST.getCode(), "Param is blank");
        }
        // todo it is very simple implementation
        return query.substring(param.length() + 1);
    }

    void writeResponse(final HttpExchange httpExchange, final OperationResponse response) throws IOException {
        try {
            httpExchange.getResponseHeaders().put(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON));
            final byte[] jsonBytes = response.toJson().getBytes();
            httpExchange.sendResponseHeaders(OK.getCode(), jsonBytes.length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(jsonBytes);
            outputStream.flush();
        } catch (Exception e) {
            throw new InvalidRequestException(BAD_REQUEST.getCode(), e.getMessage());
        }
    }

    private void handle(final Throwable throwable, final HttpExchange httpExchange) {
        try {
            final OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(getErrorResponse(throwable, httpExchange).toJson().getBytes());
            responseBody.close();
            throwable.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OperationResponse getErrorResponse(final Throwable throwable, final HttpExchange httpExchange) throws IOException {
        final OperationResponse response = new OperationResponse.Builder<>().error(throwable.getMessage()).build();
        if (throwable instanceof InvalidRequestException) {
            final InvalidRequestException exception = (InvalidRequestException) throwable;
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else if (throwable instanceof ResourceNotFoundException) {
            final ResourceNotFoundException exception = (ResourceNotFoundException) throwable;
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else if (throwable instanceof MethodNotAllowedException) {
            final MethodNotAllowedException exception = (MethodNotAllowedException) throwable;
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else {
            final InternalServerErrorException exception = (InternalServerErrorException) throwable;
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        }

        return response;
    }

    static abstract class AbstractBuilder {
        private final ConfigService configService;

        AbstractBuilder(final ConfigService configService) {
            this.configService = configService;
        }

        abstract AbstractController build();
    }
}
