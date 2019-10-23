package com.github.akarazhev.metaconfig.engine.web.internal;

final class MethodNotAllowedException extends ConfigException {

    MethodNotAllowedException(int code, String message) {
        super(code, message);
    }
}
