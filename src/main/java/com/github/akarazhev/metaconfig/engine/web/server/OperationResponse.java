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
package com.github.akarazhev.metaconfig.engine.web.server;

import com.github.akarazhev.metaconfig.extension.ExtJsonable;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.Writer;

/**
 * The operation response model that contains a result, an error message and a flag of success.
 */
final class OperationResponse<T> implements ExtJsonable {
    private final boolean success;
    private final String error;
    private final T result;

    private OperationResponse(final Builder<T> builder) {
        this.success = builder.success;
        this.error = builder.error;
        this.result = builder.result;
    }

    /**
     * Returns a success flag.
     *
     * @return an operation response success.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns an error message.
     *
     * @return an operation response error.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns a result of the request.
     *
     * @return an operation response result.
     */
    public T getResult() {
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toJson(final Writer writer) throws IOException {
        final JsonObject json = new JsonObject();
        json.put("success", success);
        json.put("error", error);
        json.put("result", result instanceof Jsonable ? ((Jsonable) result).toJson() : result);
        json.toJson(writer);
    }

    /**
     * Wraps and builds the instance of the operation response model.
     */
    final static class Builder<T> {
        private boolean success;
        private String error;
        private T result;

        /**
         * Constructs a operation response model with the result parameter.
         *
         * @param result an operation response result.
         * @return a builder of the operation response model.
         */
        Builder result(final T result) {
            this.success = true;
            this.result = Validator.of(result).get();
            return this;
        }

        /**
         * Constructs a operation response model with the error parameter.
         *
         * @param error an operation response error.
         * @return a builder of the operation response model.
         */
        Builder error(final String error) {
            this.success = false;
            this.error = Validator.of(error).get();
            return this;
        }

        /**
         * Builds a operation response model with required parameters.
         *
         * @return a builder of the operation response model.
         */
        OperationResponse<T> build() {
            return new OperationResponse<>(this);
        }
    }
}
