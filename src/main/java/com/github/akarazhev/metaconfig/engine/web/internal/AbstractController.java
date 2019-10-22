package com.github.akarazhev.metaconfig.engine.web.internal;

import com.sun.net.httpserver.HttpExchange;

abstract class AbstractController {
    final static class API {
        static final String STATUS = "/api/config/status";
    }

    void handle(HttpExchange exchange) {
        try {
            execute(exchange);
        } catch (Exception e) {
            handle(e, exchange);
        } finally {

        }
    }

    abstract void execute(final HttpExchange httpExchange) throws Exception;

    private void handle(final Throwable throwable, final HttpExchange httpExchange) {

    }
}
