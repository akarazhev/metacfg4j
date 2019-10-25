/* Copyright 2019 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.engine.web.internal;

import com.github.akarazhev.metaconfig.extension.ExtJsonable;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

final class OperationResponse<T> implements ExtJsonable {
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
        return result;
    }

    @Override
    public void toJson(Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("success", success);
        json.put("error", error);
        json.put("result", result instanceof Jsonable ? ((Jsonable) result).toJson() : result);
        json.toJson(writer);
    }

    final static class Builder<T> {
        private boolean success;
        private String error;
        private T result;

        Builder() {
        }

        Builder result(final T result) {
            this.success = true;
            this.result = Objects.requireNonNull(result);
            return this;
        }

        Builder error(final String error) {
            this.success = false;
            this.error = Objects.requireNonNull(error);
            return this;
        }

        OperationResponse<T> build() {
            return new OperationResponse<>(this);
        }
    }
}
