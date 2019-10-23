package com.github.akarazhev.metaconfig.engine.web.internal;

final class InvalidRequestException extends ConfigException {

    InvalidRequestException(int code, String message) {
        super(code, message);
    }
}
