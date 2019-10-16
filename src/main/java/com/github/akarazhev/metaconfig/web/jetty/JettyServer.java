package com.github.akarazhev.metaconfig.web.jetty;

import com.github.akarazhev.metaconfig.MetaConfig;
import com.github.akarazhev.metaconfig.web.WebServer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class JettyServer implements WebServer {
    private final static Logger logger = Logger.getLogger("JettyServer");
    private Server server;

    public JettyServer() {
        int maxThreads = 100;
        int minThreads = 10;
        int idleTimeout = 120;

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(StatusServlet.class, "/status");

        server = new Server(new QueuedThreadPool(maxThreads, minThreads, idleTimeout));
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8090);
        server.setConnectors(new Connector[] { connector });
        server.setHandler(servletHandler);
    }

    public JettyServer(final MetaConfig metaConfig) {
        // todo get the config
    }

    @Override
    public void start() throws Exception {
        server.start();
        logger.log(Level.INFO, "JettyServer started");
    }

    @Override
    public void stop() throws Exception {
        server.stop();
        logger.log(Level.INFO, "JettyServer started");
    }
}
