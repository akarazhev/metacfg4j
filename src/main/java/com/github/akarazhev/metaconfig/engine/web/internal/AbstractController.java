package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.engine.web.internal.exception.InternalServerErrorException;
import com.github.akarazhev.metaconfig.engine.web.internal.exception.InvalidRequestException;
import com.github.akarazhev.metaconfig.engine.web.internal.exception.MethodNotAllowedException;
import com.github.akarazhev.metaconfig.engine.web.internal.exception.ResourceNotFoundException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

import static com.github.akarazhev.metaconfig.engine.web.internal.constant.Constants.APPLICATION_JSON;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.Constants.CONTENT_TYPE;

abstract class AbstractController {

    protected final ConfigService configService;

    AbstractController(final ConfigService configService) {
        this.configService = configService;
    }

    void handle(HttpExchange exchange) {
        try {
            execute(exchange);
        } catch (Exception exception) {
            handle(exception, exchange);
        } finally {
            exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
        }
    }

    abstract void execute(final HttpExchange httpExchange) throws Exception;

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
