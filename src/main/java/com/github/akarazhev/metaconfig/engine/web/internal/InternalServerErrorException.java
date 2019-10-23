package com.github.akarazhev.metaconfig.engine.web.internal;

final class InternalServerErrorException extends ConfigException {

    InternalServerErrorException(int code, String message) {
        super(code, message);
    }
}
