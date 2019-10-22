package com.github.akarazhev.metaconfig.engine.web.internal.exception;

class ConfigException extends RuntimeException {
    private final int code;

    ConfigException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
