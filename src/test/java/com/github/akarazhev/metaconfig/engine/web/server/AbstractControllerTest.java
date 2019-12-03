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

import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.ConfigService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Abstract controller test")
final class AbstractControllerTest {

    private final TestController testController = new TestController.Builder(new ConfigService() {
        @Override
        public Stream<Config> update(Stream<Config> stream) {
            return null;
        }

        @Override
        public Stream<String> getNames() {
            return null;
        }

        @Override
        public Stream<Config> get() {
            return null;
        }

        @Override
        public Stream<Config> get(Stream<String> stream) {
            return null;
        }

        @Override
        public int remove(Stream<String> stream) {
            return 0;
        }

        @Override
        public void accept(String name) {
            // Empty implementation
        }

        @Override
        public void addConsumer(Consumer<Config> consumer) {
            // Empty implementation
        }
    }).build();

    @Test
    @DisplayName("Get path params")
    void getPathParams() {
        final Stream<String> params = testController.getPathParams("path", "api");
        // Check test results
        assertEquals(0, params.count());
    }

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

            @Override
            public Headers getRequestHeaders() {
                return null;
            }

            @Override
            public Headers getResponseHeaders() {
                return null;
            }

            @Override
            public URI getRequestURI() {
                return null;
            }

            @Override
            public String getRequestMethod() {
                return null;
            }

            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            @Override
            public void close() {
                // Empty implementation
            }

            @Override
            public InputStream getRequestBody() {
                return null;
            }

            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            @Override
            public void sendResponseHeaders(int i, long l) throws IOException {
                // Empty implementation
            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public void setAttribute(String s, Object o) {
                // Empty implementation
            }

            @Override
            public void setStreams(InputStream inputStream, OutputStream outputStream) {
                // Empty implementation
            }

            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        });
    }
}

class TestController extends AbstractController {

    private TestController(final Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void execute(HttpExchange httpExchange) {
        throw new RuntimeException();
    }

    /**
     * {@inheritDoc}
     */
    static class Builder extends AbstractBuilder {
        /**
         * {@inheritDoc}
         */
        Builder(final ConfigService configService) {
            super(configService);
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
