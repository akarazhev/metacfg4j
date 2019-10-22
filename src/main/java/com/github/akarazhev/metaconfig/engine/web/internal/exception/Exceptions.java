package com.github.akarazhev.metaconfig.engine.web.internal.exception;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.BAD_REQUEST;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.INTERNAL_SERVER_ERROR;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.METHOD_NOT_ALLOWED;
import static com.github.akarazhev.metaconfig.engine.web.internal.constant.StatusCodes.NOT_FOUND;

public final class Exceptions {

    private Exceptions() {
        // Factory class
    }

    public static Function<? super Throwable, ConfigException> badRequest() {
        return thr -> new InvalidRequestException(BAD_REQUEST.getCode(), thr.getMessage());
    }

    public static Supplier<ConfigException> methodNotAllowed(String message) {
        return () -> new MethodNotAllowedException(METHOD_NOT_ALLOWED.getCode(), message);
    }

    public static Supplier<ConfigException> notFound(String message) {
        return () -> new ResourceNotFoundException(NOT_FOUND.getCode(), message);
    }

    public static Supplier<ConfigException> internalServerError(String message) {
        return () -> new InternalServerErrorException(INTERNAL_SERVER_ERROR.getCode(), message);
    }
}
