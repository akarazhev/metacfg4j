package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.internal.ConfigConstants.CONTENT_TYPE;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.StatusCodes.OK;

abstract class AbstractController {

    protected final ConfigService configService;

    AbstractController(final ConfigService configService) {
        this.configService = configService;
    }

    void handle(HttpExchange httpExchange) {
        try {
            execute(httpExchange);
        } catch (Exception exception) {
            handle(exception, httpExchange);
        }
    }

    abstract void execute(final HttpExchange httpExchange) throws Exception;

    <T> void writeResponse(final HttpExchange httpExchange, final OperationResponse<T> response) {
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

    private OperationResponse getErrorResponse(Throwable throwable, HttpExchange httpExchange) throws IOException {
        OperationResponse response;
        if (throwable instanceof InvalidRequestException) {
            InvalidRequestException exception = (InvalidRequestException) throwable;
            response = new OperationResponse(false, exception.getMessage());
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else if (throwable instanceof ResourceNotFoundException) {
            ResourceNotFoundException exception = (ResourceNotFoundException) throwable;
            response = new OperationResponse(false, exception.getMessage());
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else if (throwable instanceof MethodNotAllowedException) {
            MethodNotAllowedException exception = (MethodNotAllowedException) throwable;
            response = new OperationResponse(false, exception.getMessage());
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        } else {
            InternalServerErrorException exception = (InternalServerErrorException) throwable;
            response = new OperationResponse(false, exception.getMessage());
            httpExchange.sendResponseHeaders(exception.getCode(), 0);
        }

        return response;
    }
}
