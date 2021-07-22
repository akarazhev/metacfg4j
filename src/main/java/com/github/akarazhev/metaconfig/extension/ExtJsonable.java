/* Copyright 2019-2021 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.extension;

import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.StringWriter;

import static com.github.akarazhev.metaconfig.Constants.Messages.STRING_TO_JSON_ERROR;

/**
 * Extends the basic interface of <code>Jsonable</code>.
 *
 * @see Jsonable for more information.
 */
public interface ExtJsonable extends Jsonable {

    /**
     * The default implementation of the toJson method.
     *
     * @see Jsonable for more information.
     */
    @Override
    default String toJson() {
        final var writable = new StringWriter();
        try {
            toJson(writable);
        } catch (final IOException e) {
            throw new RuntimeException(STRING_TO_JSON_ERROR, e);
        }

        return writable.toString();
    }
}
