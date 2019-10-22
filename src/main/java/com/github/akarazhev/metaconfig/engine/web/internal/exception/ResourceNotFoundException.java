package com.github.akarazhev.metaconfig.engine.web.internal.exception;

public final class ResourceNotFoundException extends ConfigException {

    ResourceNotFoundException(int code, String message) {
        super(code, message);
    }
}
