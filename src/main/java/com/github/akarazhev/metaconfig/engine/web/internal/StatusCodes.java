package com.github.akarazhev.metaconfig.engine.web.internal;

enum StatusCodes {
    OK(200),
    CREATED(201),
    ACCEPTED(202),

    BAD_REQUEST(400),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    INTERNAL_SERVER_ERROR(500);

    private final int code;

    StatusCodes(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
