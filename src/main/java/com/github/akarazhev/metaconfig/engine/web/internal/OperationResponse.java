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

    private OperationResponse(final Builder<T> builder) {
        this.success = builder.success;
        this.error = builder.error;
        this.result = builder.result;
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

    public final static class Builder<T> {
        private boolean success = true;
        private String error;
        private T result;

        public Builder() {
        }

        public Builder result(final T result) {
            this.result = Objects.requireNonNull(result);
            return this;
        }

        public Builder error(final boolean success, final String error) {
            this.success = success;
            this.error = Objects.requireNonNull(error);
            return this;
        }

        public OperationResponse<T> build() {
            return new OperationResponse<>(this);
        }
    }
}
