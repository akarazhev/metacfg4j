/* Copyright 2019 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web.server;

import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.extension.URLUtils;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.engine.web.Constants.Header.APPLICATION_JSON;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Provides a basic functionality for all controllers.
 */
abstract class AbstractController {
    private final static Logger LOGGER = Logger.getLogger(AbstractController.class.getSimpleName());
    final static String REQ_PARAM_NAMES = "names";
    final String apiPath;
    final ConfigService configService;

    AbstractController(final AbstractBuilder abstractBuilder) {
        this.apiPath = abstractBuilder.apiPath;
        this.configService = abstractBuilder.configService;
    }

    /**
     * Handles a referred context.
     *
     * @param httpExchange a http exchange.
     * @see HttpExchange for more information.
     */
    void handle(final HttpExchange httpExchange) {
        try {
            execute(httpExchange);
        } catch (final Exception e) {
            handle(httpExchange, e);
        } finally {
            httpExchange.close();
        }
    }

    /**
     * The main method that handles a context.
     *
     * @param httpExchange a http exchange.
     * @throws IOException when a controller encounters a problem.
     * @see HttpExchange for more information.
     */
    abstract void execute(final HttpExchange httpExchange) throws IOException;

    /**
     * Returns path params.
     *
     * @param path an URI path.
     * @param api  a based API.
     * @return a stream of path params.
     */
    Stream<String> getPathParams(final String path, final String api) {
        return path.contains(api) ?
                Arrays.stream(path.substring(api.length() + 1).split("/")).map(param ->
                        URLUtils.decode(param, StandardCharsets.UTF_8)) :
                Stream.empty();
    }

    /**
     * Returns a param that is a part of a request.
     *
     * @param query an URI query.
     * @param param a param to get a value of.
     * @return a value of a param.
     */
    Optional<String> getRequestParam(final String query, final String param) {
        return query != null ?
                Arrays.stream(query.split("&")).
                        filter(q -> q.contains(param)).
                        map(p -> p.split("=")[1]).
                        findFirst() :
                Optional.empty();
    }

    /**
     * Returns values belong to the param.
     *
     * @param param a param to get a value of.
     * @return a stream of values.
     * @throws JsonException when a parser encounters a problem.
     */
    Stream<String> getValues(final String param) throws JsonException {
        final String json = new String(Base64.getDecoder().decode(param), StandardCharsets.UTF_8);
        return ((JsonArray) Jsoner.deserialize(json)).stream().map(Objects::toString);
    }

    /**
     * Writes an operation response.
     *
     * @param httpExchange a http exchange.
     * @param response     an operation response.
     * @throws IOException when a controller encounters a problem.
     * @see HttpExchange for more information.
     */
    void writeResponse(final HttpExchange httpExchange, final OperationResponse response) throws IOException {
        try {
            httpExchange.getResponseHeaders().put("Content-Type", Collections.singletonList(APPLICATION_JSON));
            final byte[] jsonBytes = response.toJson().getBytes();
            httpExchange.sendResponseHeaders(HTTP_OK, jsonBytes.length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(jsonBytes);
            outputStream.flush();
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new InvalidRequestException(HTTP_BAD_REQUEST, e.getMessage());
        }
    }

    private void handle(final HttpExchange httpExchange, final Throwable throwable) {
        try {
            LOGGER.log(Level.WARNING, throwable.getMessage());
            throwable.printStackTrace();

            final OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(getErrorResponse(throwable, httpExchange).toJson().getBytes());
            responseBody.close();
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, throwable.getMessage());
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
            if (throwable instanceof InternalServerErrorException) {
                httpExchange.sendResponseHeaders(((InternalServerErrorException) throwable).getCode(), 0);
            } else {
                httpExchange.sendResponseHeaders(HTTP_INTERNAL_ERROR, 0);
            }
        }

        return response;
    }

    /**
     * Wraps and builds instances of controllers.
     */
    static abstract class AbstractBuilder {
        private final String apiPath;
        private final ConfigService configService;

        /**
         * Constructs a controller with the configuration service param.
         *
         * @param apiPath       an api path.
         * @param configService a configuration service.
         */
        AbstractBuilder(final String apiPath, final ConfigService configService) {
            this.apiPath = Validator.of(apiPath).get();
            this.configService = Validator.of(configService).get();
        }

        /**
         * Builds a controller with the required parameter.
         *
         * @return a builder of the controller.
         */
        abstract AbstractController build();
    }
}
