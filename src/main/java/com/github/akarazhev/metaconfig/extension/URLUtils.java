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
package com.github.akarazhev.metaconfig.extension;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_UTILS_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_DECODING_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_ENCODING_ERROR;

/**
 * Encodes and decodes URL params.
 */
public final class URLUtils {

    private URLUtils() {
        throw new AssertionError(CREATE_UTILS_CLASS_ERROR);
    }

    /**
     * Encodes an URL param.
     *
     * @param param   a param to encode.
     * @param charset a charset.
     * @return an encoded param.
     */
    public static String encode(final String param, final Charset charset) {
        try {
            return URLEncoder.encode(param, charset.toString());
        } catch (final Exception e) {
            throw new RuntimeException(PARAM_ENCODING_ERROR, e);
        }
    }

    /**
     * Decodes an URL param.
     *
     * @param param   a param to decode.
     * @param charset a charset.
     * @return a decoded param.
     */
    public static String decode(final String param, final Charset charset) {
        try {
            return URLDecoder.decode(param, charset.toString());
        } catch (final Exception e) {
            throw new RuntimeException(PARAM_DECODING_ERROR, e);
        }
    }
}
