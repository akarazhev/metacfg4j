package com.github.akarazhev.metaconfig.engine.web.internal;

import java.io.IOException;

class ConfigException extends IOException {
    private final int code;

    ConfigException(int code, String message) {
        super(message);
        this.code = code;
    }

    int getCode() {
        return code;
    }
}
