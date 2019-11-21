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

import java.io.IOException;

/**
 * Extends a standard <code>IOException</code> to be used as a business internal exception.
 */
class ConfigException extends IOException {
    private final int code;

    /**
     * Constructs an exception with a code and a message.
     *
     * @param code a code.
     * @param message a message.
     */
    ConfigException(final int code, final String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns a code of the exception.
     *
     * @return a value of the code.
     */
    int getCode() {
        return code;
    }
}
