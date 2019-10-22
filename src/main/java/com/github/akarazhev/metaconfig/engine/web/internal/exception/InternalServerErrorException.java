package com.github.akarazhev.metaconfig.engine.web.internal.exception;

public final class InternalServerErrorException extends ConfigException {

    InternalServerErrorException(int code, String message) {
        super(code, message);
    }
}
