/* Copyright 2019-2022 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.extension;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.akarazhev.metaconfig.Constants.Messages.CREATE_UTILS_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_DECODING_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_ENCODING_ERROR;

/**
 * Contains functions for URL/URI processing.
 */
public final class WebUtils {

    private WebUtils() {
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

    /**
     * Returns path params.
     *
     * @param uri an URI with the path.
     * @param api a based API.
     * @return a stream of path params.
     */
    public static Stream<String> getPathParams(final URI uri, final String api) {
        final String path = uri.getPath();
        return path.contains(api) ?
                Arrays.stream(path.substring(api.length() + 1).split("/")).map(param ->
                        decode(param, StandardCharsets.UTF_8)) :
                Stream.empty();
    }

    /**
     * Returns a param that is a part of a request.
     *
     * @param uri   an URI with the query.
     * @param param a param to get a value of.
     * @return a value of a param.
     */
    public static Optional<String> getRequestParam(final URI uri, final String param) {
        final String query = uri.getQuery();
        return query != null ?
                Arrays.stream(query.split("&")).
                        filter(q -> q.contains(param)).
                        map(p -> decode(p.split("=")[1], StandardCharsets.UTF_8)).
                        findFirst() :
                Optional.empty();
    }

    /**
     * Returns values belong to the param.
     *
     * @param param a param to get a value of.
     * @return a stream of values.
     * @throws JsonException when a parser encounters a problem.
     */
    public static Stream<String> getValues(final String param) throws JsonException {
        final String json = new String(Base64.getDecoder().decode(param), StandardCharsets.UTF_8);
        return ((JsonArray) Jsoner.deserialize(json)).stream().map(Objects::toString);
    }

    /**
     * Returns a value belongs to the param.
     *
     * @param param a param to get a value of.
     * @return a json value.
     * @throws JsonException when a parser encounters a problem.
     */
    public static JsonObject getValue(final String param) throws JsonException {
        final String json = new String(Base64.getDecoder().decode(param), StandardCharsets.UTF_8);
        return (JsonObject) Jsoner.deserialize(json);
    }
}
