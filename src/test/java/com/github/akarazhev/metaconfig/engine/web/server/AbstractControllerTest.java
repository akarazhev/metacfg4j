/* Copyright 2019-2020 Andrey Karazhev
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

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
import com.github.akarazhev.metaconfig.api.PageRequest;
import com.github.akarazhev.metaconfig.api.PageResponse;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Abstract controller test")
final class AbstractControllerTest {

    private final TestController testController = new TestController.Builder("/api/metacfg/", new ConfigService() {

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<Config> update(final Stream<Config> stream) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<String> getNames() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PageResponse getNames(final PageRequest request) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<Config> get() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<Config> get(final Stream<String> stream) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int remove(final Stream<String> stream) {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept(final Stream<String> stream) {
            // Empty implementation
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addConsumer(final Consumer<Config> consumer) {
            // Empty implementation
        }
    }).build();

    @Test
    @DisplayName("Write response")
    void writeResponse() {
        assertThrows(IOException.class, () ->
                testController.writeResponse(null, new OperationResponse.Builder<>().result("Ok").build()));
    }

    @Test
    @DisplayName("Handle")
    void handle() {
        testController.handle(new HttpExchange() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Headers getRequestHeaders() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Headers getResponseHeaders() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public URI getRequestURI() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getRequestMethod() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void close() {
                // Empty implementation
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public InputStream getRequestBody() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void sendResponseHeaders(final int i, final long l) {
                // Empty implementation
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int getResponseCode() {
                return 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getProtocol() {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getAttribute(final String s) {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setAttribute(final String s, final Object o) {
                // Empty implementation
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setStreams(final InputStream inputStream, final OutputStream outputStream) {
                // Empty implementation
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        });
    }
}

final class TestController extends AbstractController {

    private TestController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(final HttpExchange httpExchange) {
        throw new RuntimeException("Error");
    }

    /**
     * {@inheritDoc}
     */
    final static class Builder extends AbstractBuilder {

        /**
         * {@inheritDoc}
         */
        Builder(final String apiPath, final ConfigService configService) {
            super(apiPath, configService);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        TestController build() {
            return new TestController(this);
        }
    }
}
