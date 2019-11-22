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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.akarazhev.metaconfig.Constants.CREATE_CONSTANT_CLASS_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.CERTIFICATE_LOAD_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_CREATE_ERROR;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STARTED;
import static com.github.akarazhev.metaconfig.Constants.Messages.SERVER_STOPPED;
import static com.github.akarazhev.metaconfig.Constants.Messages.WRONG_CONFIG_NAME;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.ACCEPT_CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG;
import static com.github.akarazhev.metaconfig.engine.web.Constants.API.CONFIG_NAMES;

/**
 * The internal implementation of the web server.
 */
public final class ConfigServer implements WebServer {
    private final static Logger LOGGER = Logger.getLogger(ConfigServer.class.getSimpleName());
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
        // The port key
        public static final String PORT = "port";
        // The port value
        static final int PORT_VALUE = 8000;
        // The backlog key
        public static final String BACKLOG = "backlog";
        // The backlog value
        public static final int BACKLOG_VALUE = 0;
        // The keyStoreFile key
        public static final String KEY_STORE_FILE = "keyStoreFile";
        // The keyStoreFile value
        static final String KEY_STORE_FILE_VALUE = "./data/metacfg4j.keystore";
        // The alias key
        public static final String ALIAS = "alias";
        // The alias value
        static final String ALIAS_VALUE = "alias";
        // The store password key
        public static final String STORE_PASSWORD = "storePassword";
        // The store password value
        static final String STORE_PASSWORD_VALUE = "password";
        // The key password key
        public static final String KEY_PASSWORD = "keyPassword";
        // The key password value
        static final String KEY_PASSWORD_VALUE = "password";
    }

    /**
     * Constructs a default web server.
     *
     * @param configService a configuration service.
     * @throws Exception when a web server encounters a problem.
     */
    public ConfigServer(final ConfigService configService) throws Exception {
        // Set the default config
        this(new Config.Builder(Settings.CONFIG_NAME, Arrays.asList(
                new Property.Builder(Settings.PORT, Settings.BACKLOG_VALUE).build(),
                new Property.Builder(Settings.BACKLOG, Settings.PORT_VALUE).build(),
                new Property.Builder(Settings.KEY_STORE_FILE, Settings.KEY_STORE_FILE_VALUE).build(),
                new Property.Builder(Settings.ALIAS, Settings.ALIAS_VALUE).build(),
                new Property.Builder(Settings.STORE_PASSWORD, Settings.STORE_PASSWORD_VALUE).build(),
                new Property.Builder(Settings.KEY_PASSWORD, Settings.KEY_PASSWORD_VALUE).build())).build(), configService);
    }

    /**
     * Constructs a web server based on the configuration.
     *
     * @param config        config a configuration of a web server.
     * @param configService a configuration service.
     * @throws Exception when a web server encounters a problem.
     */
    public ConfigServer(final Config config, final ConfigService configService) throws Exception {
        // Validate the config
        final Config serverConfig = Validator.of(config).
                validate(c -> Settings.CONFIG_NAME.equals(c.getName()), WRONG_CONFIG_NAME).
                validate(c -> c.getProperty(Settings.PORT).isPresent(), "Port is not presented.").
                validate(c -> c.getProperty(Settings.BACKLOG).isPresent(), "Backlog is not presented.").
                validate(c -> c.getProperty(Settings.KEY_STORE_FILE).isPresent(), "Key store file is not presented.").
                validate(c -> c.getProperty(Settings.ALIAS).isPresent(), "Alias is not presented.").
                validate(c -> c.getProperty(Settings.STORE_PASSWORD).isPresent(), "Store password is not presented.").
                validate(c -> c.getProperty(Settings.KEY_PASSWORD).isPresent(), "Key password is not presented.").
                get();
        // Get the port
        final int port = serverConfig.getProperty(Settings.PORT).
                map(property -> (int) property.asLong()).
                orElse(8000);
        // Get the backlog
        final int backlog = serverConfig.getProperty(Settings.BACKLOG).
                map(property -> (int) property.asLong()).
                orElse(0);
        // Init the server
        httpsServer = HttpsServer.create(new InetSocketAddress(port), backlog);
        httpsServer.createContext(ACCEPT_CONFIG, new AcceptConfigController.Builder(configService).build()::handle);
        httpsServer.createContext(CONFIG_NAMES, new ConfigNamesController.Builder(configService).build()::handle);
        httpsServer.createContext(CONFIG, new ConfigController.Builder(configService).build()::handle);
        httpsServer.setExecutor(null);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(getSSLContext(serverConfig)) {

            public void configure(final HttpsParameters params) {
                try {
                    final SSLContext sslContext = SSLContext.getDefault();
                    final SSLEngine sslEngine = sslContext.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(sslEngine.getEnabledCipherSuites());
                    params.setProtocols(sslEngine.getEnabledProtocols());
                    final SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception e) {
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
        final Optional<Property> keyStoreFile = serverConfig.getProperty(Settings.KEY_STORE_FILE);
        if (!keyStoreFile.isPresent()) {
            throw new Exception(CERTIFICATE_LOAD_ERROR);
        }

        final List<Throwable> exceptions = new LinkedList<>();
        final FileInputStream fileInputStream = new FileInputStream(keyStoreFile.get().getValue());

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        serverConfig.getProperty(Settings.STORE_PASSWORD).ifPresent(property -> {
            try {
                keyStore.load(fileInputStream, property.getValue().toCharArray());
            } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                exceptions.add(e);
            }
        });

        serverConfig.getProperty(Settings.ALIAS).ifPresent(property -> {
            try {
                LOGGER.log(Level.INFO, keyStore.getCertificate(property.getValue()).toString());
            } catch (KeyStoreException e) {
                exceptions.add(e);
            }
        });

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        serverConfig.getProperty(Settings.STORE_PASSWORD).ifPresent(property -> {
            try {
                keyManagerFactory.init(keyStore, property.getValue().toCharArray());
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
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
