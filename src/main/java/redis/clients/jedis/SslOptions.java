/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redis.clients.jedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import redis.clients.jedis.util.InsecureTrustManagerFactory;

/**
 * Options to configure SSL options for the connections kept to Redis servers.
 *
 * @author Mark Paluch
 */
public class SslOptions {

    private final String keyManagerAlgorithm;

    private final String trustManagerAlgorithm;

    private final String keyStoreType;

    private final String trustStoreType;

    private final Resource keystoreResource;

    private final char[] keystorePassword;

    private final Resource truststoreResource;

    private final char[] truststorePassword;

    private final SSLParameters sslParameters;

    private final SslVerifyMode sslVerifyMode;

    private SslOptions(Builder builder) {
        this.keyManagerAlgorithm = builder.keyManagerAlgorithm;
        this.trustManagerAlgorithm = builder.trustManagerAlgorithm;
        this.keyStoreType = builder.keyStoreType;
        this.trustStoreType = builder.trustStoreType;
        this.keystoreResource = builder.keystoreResource;
        this.keystorePassword = builder.keystorePassword;
        this.truststoreResource = builder.truststoreResource;
        this.truststorePassword = builder.truststorePassword;
        this.sslParameters = builder.sslParameters;
        this.sslVerifyMode = builder.sslVerifyMode;
    }

    /**
     * Returns a new {@link SslOptions.Builder} to construct {@link SslOptions}.
     *
     * @return a new {@link SslOptions.Builder} to construct {@link SslOptions}.
     */
    public static SslOptions.Builder builder() {
        return new SslOptions.Builder();
    }

    /**
     * Builder for {@link SslOptions}.
     */
    public static class Builder {

        private String keyManagerAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        //private String keyManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm(); // Lettuce

        private String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

        private String keyStoreType;

        private String trustStoreType;

        private Resource keystoreResource;

        private char[] keystorePassword = new char[0];

        private Resource truststoreResource;

        private char[] truststorePassword = new char[0];

        private SSLParameters sslParameters;

        private SslVerifyMode sslVerifyMode = SslVerifyMode.FULL;

        private Builder() {
        }

        public Builder keyManagerAlgorithm(String keyManagerAlgorithm) {
            this.keyManagerAlgorithm = Objects.requireNonNull(keyManagerAlgorithm, "KeyManagerAlgorithm must not be null");
            return this;
        }

        public Builder systemDefaultKeyManager() {
            this.keyManagerAlgorithm = null;
            return this;
        }

        public Builder trustManagerAlgorithm(String trustManagerAlgorithm) {
            this.trustManagerAlgorithm = Objects.requireNonNull(trustManagerAlgorithm, "TrustManagerAlgorithm must not be null");
            return this;
        }

        public Builder systemDefaultTrustManager() {
            this.trustManagerAlgorithm = null;
            return this;
        }

        /**
         * Sets the KeyStore type. Defaults to {@link KeyStore#getDefaultType()} if not set.
         *
         * @param keyStoreType the keystore type to use, must not be {@code null}.
         * @return {@code this}
         */
        public Builder keyStoreType(String keyStoreType) {
            this.keyStoreType = Objects.requireNonNull(keyStoreType, "KeyStoreType must not be null");
            return this;
        }

        /**
         * Sets the KeyStore type. Defaults to {@link KeyStore#getDefaultType()} if not set.
         *
         * @param trustStoreType the keystore type to use, must not be {@code null}.
         * @return {@code this}
         */
        public Builder trustStoreType(String trustStoreType) {
            this.trustStoreType = Objects.requireNonNull(trustStoreType, "TrustStoreType must not be null");
            return this;
        }

        /**
         * Sets the Keystore file to load client certificates. The key store file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @return {@code this}
         */
        public Builder keystore(File keystore) {
            return keystore(keystore, null);
            //return keystore(keystore, new char[0]); // Lettuce
        }

        /**
         * Sets the Keystore file to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         */
        public Builder keystore(File keystore, char[] keystorePassword) {

            Objects.requireNonNull(keystore, "Keystore must not be null");
//            LettuceAssert.isTrue(keystore.exists(), () -> String.format("Keystore file %s does not exist", truststore));
//            LettuceAssert.isTrue(keystore.isFile(), () -> String.format("Keystore %s is not a file", truststore));

            return keystore(Resource.from(keystore), keystorePassword);
        }

        /**
         * Sets the Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore URL, must not be {@code null}.
         * @return {@code this}
         */
        public Builder keystore(URL keystore) {
            return keystore(keystore, null);
        }

        /**
         * Sets the Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @param keystorePassword
         * @return {@code this}
         */
        public Builder keystore(URL keystore, char[] keystorePassword) {

            Objects.requireNonNull(keystore, "Keystore must not be null");

            return keystore(Resource.from(keystore), keystorePassword);
        }

        /**
         * Sets the Java Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param resource the provider that opens a {@link InputStream} to the keystore file, must not be {@code null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         */
        public Builder keystore(Resource resource, char[] keystorePassword) {

            Objects.requireNonNull(resource, "Keystore InputStreamProvider must not be null");

            this.keystorePassword = getPassword(keystorePassword);
            this.keystoreResource = resource;

            return this;
        }

        /**
         * Sets the Truststore file to load trusted certificates. The truststore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @return {@code this}
         */
        public Builder truststore(File truststore) {
            return truststore(truststore, null);
        }

        /**
         * Sets the Truststore file to load trusted certificates. The truststore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(File truststore, String truststorePassword) {

            Objects.requireNonNull(truststore, "Truststore must not be null");
//            LettuceAssert.isTrue(truststore.exists(), () -> String.format("Truststore file %s does not exist", truststore));
//            LettuceAssert.isTrue(truststore.isFile(), () -> String.format("Truststore file %s is not a file", truststore));

            return truststore(Resource.from(truststore), getPassword(truststorePassword));
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @return {@code this}
         */
        public Builder truststore(URL truststore) {
            return truststore(truststore, null);
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(URL truststore, String truststorePassword) {

            Objects.requireNonNull(truststore, "Truststore must not be null");

            return truststore(Resource.from(truststore), getPassword(truststorePassword));
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param resource the provider that opens a {@link InputStream} to the keystore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        private Builder truststore(Resource resource, char[] truststorePassword) {

            Objects.requireNonNull(resource, "Truststore InputStreamProvider must not be null");

            this.truststorePassword = getPassword(truststorePassword);
            this.truststoreResource = resource;

            return this;
        }

        public Builder sslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        public Builder sslVerifyMode(SslVerifyMode sslVerifyMode) {
            this.sslVerifyMode = sslVerifyMode;
            return this;
        }

        /**
         * Create a new instance of {@link SslOptions}
         *
         * @return new instance of {@link SslOptions}
         */
        public SslOptions build() {
            if (this.sslParameters == null) {
                this.sslParameters = new SSLParameters();
            }
            return new SslOptions(this);
        }

    }

    /**
     * A {@link SSLContext} object that is configured with values from this {@link SslOptions} object.
     *
     * @return {@link SSLContext}
     * @throws IOException thrown when loading the keystore or the truststore fails.
     * @throws GeneralSecurityException thrown when loading the keystore or the truststore fails.
     */
    public SSLContext createSslContext() throws IOException, GeneralSecurityException {

        TrustManagerFactory trustManagerFactory = null;

        if (sslVerifyMode == SslVerifyMode.FULL) {
            this.sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        } else if (sslVerifyMode == SslVerifyMode.CA) {
            this.sslParameters.setEndpointIdentificationAlgorithm("");
        } else if (sslVerifyMode == SslVerifyMode.INSECURE) {
            trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
        }

        KeyManagerFactory keyManagerFactory = null;
        if (keyManagerAlgorithm != null) {

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            try (InputStream keystoreStream = keystoreResource.get()) {
                keyStore.load(keystoreStream, keystorePassword);
            }

            keyManagerFactory = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            keyManagerFactory.init(keyStore, keystorePassword);
        }

        if (trustManagerFactory != null) {
            // skip
        } else if (trustManagerAlgorithm == null) {

            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            try (InputStream truststoreStream = truststoreResource.get()) {
                trustStore.load(truststoreStream, truststorePassword);
            }

            trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
            trustManagerFactory.init(trustStore);
        }

        SSLContext sslContext = SSLContext.getDefault();
        //SSLContext sslContext = SSLContext.getInstance("TLS"); // examples

        sslContext.init(
                keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(),
                trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(),
                null);

        return sslContext;
    }

    /**
     * {@link #createSslContext()} must be called before this.
     * @return {@link SSLParameters}
     */
    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static char[] getPassword(String truststorePassword) {
        return !isEmpty(truststorePassword) ? truststorePassword.toCharArray() : null;
    }

    private static char[] getPassword(char[] chars) {
        return chars != null ? Arrays.copyOf(chars, chars.length) : null;
    }

    /**
     * Supplier for a {@link InputStream} representing a resource. The resulting {@link InputStream} must be closed by
     * the calling code.
     */
    @FunctionalInterface
    public interface Resource {

        /**
         * Create a {@link Resource} that obtains a {@link InputStream} from a {@link URL}.
         *
         * @param url the URL to obtain the {@link InputStream} from.
         * @return a {@link Resource} that opens a connection to the URL and obtains the {@link InputStream} for it.
         */
        static Resource from(URL url) {

            Objects.requireNonNull(url, "URL must not be null");

            return () -> url.openConnection().getInputStream();
        }

        /**
         * Create a {@link Resource} that obtains a {@link InputStream} from a {@link File}.
         *
         * @param file the File to obtain the {@link InputStream} from.
         * @return a {@link Resource} that obtains the {@link FileInputStream} for the given {@link File}.
         */
        static Resource from(File file) {

            Objects.requireNonNull(file, "File must not be null");

            return () -> new FileInputStream(file);
        }

        /**
         * Obtains the {@link InputStream}.
         *
         * @return the {@link InputStream}.
         * @throws IOException
         */
        InputStream get() throws IOException;

    }

}
