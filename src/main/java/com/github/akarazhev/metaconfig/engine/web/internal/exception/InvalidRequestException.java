package com.github.akarazhev.metaconfig.engine.web.internal.exception;

public final class InvalidRequestException extends ConfigException {

    InvalidRequestException(int code, String message) {
        super(code, message);
    }
}
