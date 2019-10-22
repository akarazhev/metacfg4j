package com.github.akarazhev.metaconfig.engine.web.internal.exception;

public final class MethodNotAllowedException extends ConfigException {

    MethodNotAllowedException(int code, String message) {
        super(code, message);
    }
}
