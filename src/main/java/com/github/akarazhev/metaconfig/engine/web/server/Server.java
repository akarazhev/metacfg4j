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
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.engine.web.WebServer;
import com.github.akarazhev.metaconfig.extension.Validator;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.ACCEPT_CONFIG_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.CONFIG;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.CONFIG_NAMES;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.CONFIG_NAMES_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Endpoints.CONFIG_VALUE;
import static com.github.akarazhev.metaconfig.Constants.Messages.CERTIFICATE_LOAD_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.PARAM_NOT_PRESENTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_CREATE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.ALIAS;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.ALIAS_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.API_PATH;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.API_PATH_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.BACKLOG;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.BACKLOG_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.HOSTNAME;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.HOSTNAME_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.KEY_PASSWORD;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.KEY_PASSWORD_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.KEY_STORE_FILE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.KEY_STORE_FILE_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.PORT;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.PORT_VALUE;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.STORE_PASSWORD;
import static com.github.akarazhev.metaconfig.engine.web.server.Server.Settings.STORE_PASSWORD_VALUE;

/**
 * The internal implementation of the web server.
 */
public final class Server implements WebServer {
    private final static Logger LOGGER = Logger.getLogger(Server.class.getSimpleName());
    private HttpsServer httpsServer;

    /**
     * Settings constants for the web server.
     */
    public final static class Settings {

        private Settings() {
            throw new AssertionError(CREATE_CONSTANT_CLASS_ERROR);
        }

        // The configuration name
        public static final String CONFIG_NAME = "config-server";
        // The hostname key
        public static final String HOSTNAME = "hostname";
        // The hostname value
        static final String HOSTNAME_VALUE = "localhost";
        // The api path key
        static final String API_PATH = "api-path";
        // The api path value
        static final String API_PATH_VALUE = "/api/metacfg/";
        // The port key
        public static final String PORT = "port";
        // The port value
        static final int PORT_VALUE = 8000;
        // The backlog key
        public static final String BACKLOG = "backlog";
        // The backlog value
        static final int BACKLOG_VALUE = 0;
        // The key-store-file key
        public static final String KEY_STORE_FILE = "key-store-file";
        // The key-store-file value
        static final String KEY_STORE_FILE_VALUE = "./data/metacfg4j.keystore";
        // The alias key
        public static final String ALIAS = "alias";
        // The alias value
        static final String ALIAS_VALUE = "alias";
        // The store password key
        public static final String STORE_PASSWORD = "store-password";
        // The store password value
        static final String STORE_PASSWORD_VALUE = "password";
        // The key password key
        public static final String KEY_PASSWORD = "key-password";
        // The key password value
        static final String KEY_PASSWORD_VALUE = "password";
    }

    /**
     * Constructs a default web server.
     *
     * @param configService a configuration service.
     * @throws Exception when a web server encounters a problem.
     */
    public Server(final ConfigService configService) throws Exception {
        // Set the default config
        this(new Config.Builder(CONFIG_NAME, Arrays.asList(
                new Property.Builder(HOSTNAME, HOSTNAME_VALUE).build(),
                new Property.Builder(PORT, PORT_VALUE).build(),
                new Property.Builder(BACKLOG, BACKLOG_VALUE).build(),
                new Property.Builder(KEY_STORE_FILE, KEY_STORE_FILE_VALUE).build(),
                new Property.Builder(ALIAS, ALIAS_VALUE).build(),
                new Property.Builder(STORE_PASSWORD, STORE_PASSWORD_VALUE).build(),
                new Property.Builder(KEY_PASSWORD, KEY_PASSWORD_VALUE).build())).build(), configService);
    }

    /**
     * Constructs a web server based on the configuration.
     *
     * @param config        config a configuration of a web server.
     * @param configService a configuration service.
     * @throws Exception when a web server encounters a problem.
     */
    public Server(final Config config, final ConfigService configService) throws Exception {
        // Validate the config
        final Config serverConfig = Validator.of(config).
                validate(c -> CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(KEY_STORE_FILE).isPresent(), String.format(PARAM_NOT_PRESENTED, KEY_STORE_FILE)).
                validate(c -> c.getProperty(ALIAS).isPresent(), String.format(PARAM_NOT_PRESENTED, ALIAS)).
                validate(c -> c.getProperty(STORE_PASSWORD).isPresent(), String.format(PARAM_NOT_PRESENTED, STORE_PASSWORD)).
                validate(c -> c.getProperty(KEY_PASSWORD).isPresent(), String.format(PARAM_NOT_PRESENTED, KEY_PASSWORD)).
                get();
        // Get the hostname
        final String hostname = serverConfig.getProperty(HOSTNAME).
                map(Property::getValue).
                orElse(HOSTNAME_VALUE);
        // Get the api path
        final String apiPath = serverConfig.getProperty(API_PATH).
                map(Property::getValue).
                orElse(API_PATH_VALUE);
        // Get the port
        final int port = serverConfig.getProperty(PORT).
                map(property -> (int) property.asLong()).
                orElse(PORT_VALUE);
        // Get the backlog
        final int backlog = serverConfig.getProperty(BACKLOG).
                map(property -> (int) property.asLong()).
                orElse(BACKLOG_VALUE);
        // Init the server
        httpsServer = HttpsServer.create(new InetSocketAddress(hostname, port), backlog);
        // Get the accept config endpoint
        final String acceptConfigEndpoint = serverConfig.getProperty(ACCEPT_CONFIG).
                map(Property::getValue).
                orElse(ACCEPT_CONFIG_VALUE);
        final String acceptApi = apiPath + acceptConfigEndpoint;
        httpsServer.createContext(acceptApi,
                new AcceptConfigController.Builder(acceptApi, configService).build()::handle);
        // Get the config names endpoint
        final String configNamesEndpoint = serverConfig.getProperty(CONFIG_NAMES).
                map(Property::getValue).
                orElse(CONFIG_NAMES_VALUE);
        httpsServer.createContext(apiPath + configNamesEndpoint,
                new ConfigNamesController.Builder(configService).build()::handle);
        // Get the config endpoint
        final String configEndpoint = serverConfig.getProperty(CONFIG).
                map(Property::getValue).
                orElse(CONFIG_VALUE);
        httpsServer.createContext(apiPath + configEndpoint,
                new ConfigController.Builder(configService).build()::handle);
        httpsServer.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(getSSLContext(serverConfig)) {

            /**
             * {@inheritDoc}
             */
            @Override
            public void configure(final HttpsParameters params) {
                try {
                    final SSLContext sslContext = SSLContext.getDefault();
                    final SSLEngine sslEngine = sslContext.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(sslEngine.getEnabledCipherSuites());
                    params.setProtocols(sslEngine.getEnabledProtocols());
                    final SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (final Exception e) {
                    LOGGER.log(Level.SEVERE, SERVER_CREATE_ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebServer start() {
        httpsServer.start();
        LOGGER.log(Level.INFO, SERVER_STARTED);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        httpsServer.stop(0);
        LOGGER.log(Level.INFO, SERVER_STOPPED);
    }

    private SSLContext getSSLContext(final Config serverConfig) throws Exception {
        final Optional<Property> keyStoreFile = serverConfig.getProperty(KEY_STORE_FILE);
        if (!keyStoreFile.isPresent()) {
            throw new Exception(CERTIFICATE_LOAD_ERROR);
        }

        final Collection<Throwable> exceptions = new LinkedList<>();
        final FileInputStream fileInputStream = new FileInputStream(keyStoreFile.get().getValue());

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        serverConfig.getProperty(STORE_PASSWORD).ifPresent(property -> {
            try {
                keyStore.load(fileInputStream, property.getValue().toCharArray());
            } catch (final IOException | NoSuchAlgorithmException | CertificateException e) {
                exceptions.add(e);
            }
        });

        serverConfig.getProperty(ALIAS).ifPresent(property -> {
            try {
                LOGGER.log(Level.INFO, keyStore.getCertificate(property.getValue()).toString());
            } catch (final KeyStoreException e) {
                exceptions.add(e);
            }
        });

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        serverConfig.getProperty(STORE_PASSWORD).ifPresent(property -> {
            try {
                keyManagerFactory.init(keyStore, property.getValue().toCharArray());
            } catch (final KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                exceptions.add(e);
            }
        });

        if (exceptions.size() > 0) {
            throw new Exception(CERTIFICATE_LOAD_ERROR);
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}
