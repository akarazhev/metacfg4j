package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

final class OperationResponse<T> implements Jsonable {
    private final boolean success;
    private final String error;
    private final T result;

    OperationResponse(final T result) {
        this.success = true;
        this.error = null;
        this.result = Objects.requireNonNull(result);
    }

    OperationResponse(final boolean success, final String error) {
        this.success = success;
        this.error = Objects.requireNonNull(error);
        this.result = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public T getResult() {
        return result; // todo is it immutable?
    }

    @Override
    public String toJson() {
        final StringWriter writable = new StringWriter();
        try {
            toJson(writable);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return writable.toString();
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("success", success);
        json.put("error", error);
        json.put("result", result instanceof Jsonable ? ((Jsonable) result).toJson() : result);
        json.toJson(writer);
    }
}
