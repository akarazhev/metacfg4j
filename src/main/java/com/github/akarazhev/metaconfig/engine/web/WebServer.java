package com.github.akarazhev.metaconfig.engine.web;

import java.io.IOException;

public interface WebServer {

    WebServer start() throws IOException;

    void stop() throws IOException;
}
