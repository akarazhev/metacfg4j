package com.github.akarazhev.metaconfig.web.jetty;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StatusServlet extends HttpServlet {
    private final static Logger logger = Logger.getLogger("StatusServlet");
    private static final String RESPONSE = "{ \"status\": \"ok\"}";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ByteBuffer content = ByteBuffer.wrap(RESPONSE.getBytes(StandardCharsets.UTF_8));

        AsyncContext async = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        out.setWriteListener(new WriteListener() {

            @Override
            public void onWritePossible() throws IOException {
                while (out.isReady()) {
                    if (!content.hasRemaining()) {
                        response.setContentType("application/json");
                        response.setStatus(200);
                        async.complete();
                        return;
                    }

                    out.write(content.get());
                }
            }

            @Override
            public void onError(Throwable t) {
                getServletContext().log("Async Error", t);
                logger.log(Level.WARNING, "Async Error", t);
                async.complete();
            }
        });
    }
}
