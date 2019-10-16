package com.github.akarazhev.metaconfig.web.internal;

import com.github.akarazhev.metaconfig.Config;
import com.github.akarazhev.metaconfig.web.WebServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InternalServer implements WebServer {
    private final static Logger logger = Logger.getLogger("InternalServer");
    private HttpServer server;

    public InternalServer() {
        int serverPort = 8000;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
            server.createContext("/api/status", httpExchange -> {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    String response = "{ \"status\": \"ok\"}";
                    httpExchange.sendResponseHeaders(200, response.getBytes().length);
                    httpExchange.getRequestHeaders().put("Content-Type", Collections.singletonList("application/json"));
                    OutputStream outputStream = httpExchange.getResponseBody();
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                } else {
                    httpExchange.sendResponseHeaders(405, -1);
                }

                httpExchange.close();
            });
            server.setExecutor(null);
        } catch (IOException e) {
            e.printStackTrace();
            server = null;
        }
    }

    public InternalServer(final Config config) {
        server = null;
    }

    @Override
    public void start() throws Exception {
        if (server == null) {
            throw new Exception("Server was not created");
        }

        server.start();
        logger.log(Level.INFO, "Server started");
    }

    @Override
    public void stop() throws Exception {
        if (server == null) {
            throw new Exception("Server was not created");
        }

        server.stop(0);
        logger.log(Level.INFO, "Server stopped");
    }
}
