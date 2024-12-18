package redis.clients.jedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options to configure SSL options for the connections kept to Redis servers.
 *
 * @author Mark Paluch
 */
public class SslOptions {

    private static final Logger logger = LoggerFactory.getLogger(SslOptions.class);

    private final String keyManagerAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

    private final String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

    private final String keyStoreType;

    private final String trustStoreType;

    private final Resource keystoreResource;

    private final char[] keystorePassword;

    private final Resource truststoreResource;

    private final char[] truststorePassword;

    private final SSLParameters sslParameters;

    private final SslVerifyMode sslVerifyMode;

    private final String sslProtocol; // protocol for SSLContext

    private SslOptions(Builder builder) {
        this.keyStoreType = builder.keyStoreType;
        this.trustStoreType = builder.trustStoreType;
        this.keystoreResource = builder.keystoreResource;
        this.keystorePassword = builder.keystorePassword;
        this.truststoreResource = builder.truststoreResource;
        this.truststorePassword = builder.truststorePassword;
        this.sslParameters = builder.sslParameters;
        this.sslVerifyMode = builder.sslVerifyMode;
        this.sslProtocol = builder.sslProtocol;
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

        private String keyStoreType;

        private String trustStoreType;

        private Resource keystoreResource;

        private char[] keystorePassword = null;

        private Resource truststoreResource;

        private char[] truststorePassword = null;

        private SSLParameters sslParameters;

        private SslVerifyMode sslVerifyMode = SslVerifyMode.FULL;

        private String sslProtocol = "TLS"; // protocol for SSLContext

        private Builder() {
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
            assertFile("Keystore", keystore);

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

            this.keystoreResource = Objects.requireNonNull(resource, "Keystore InputStreamProvider must not be null");

            this.keystorePassword = getPassword(keystorePassword);

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
        public Builder truststore(File truststore, char[] truststorePassword) {

            Objects.requireNonNull(truststore, "Truststore must not be null");
            assertFile("Truststore", truststore);

            return truststore(Resource.from(truststore), truststorePassword);
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
        public Builder truststore(URL truststore, char[] truststorePassword) {

            Objects.requireNonNull(truststore, "Truststore must not be null");

            return truststore(Resource.from(truststore), truststorePassword);
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
        public Builder truststore(Resource resource, char[] truststorePassword) {

            this.truststoreResource = Objects.requireNonNull(resource, "Truststore InputStreamProvider must not be null");

            this.truststorePassword = getPassword(truststorePassword);

            return this;
        }

        /**
         * Sets a configured {@link SSLParameters}.
         *
         * @param sslParameters a {@link SSLParameters} object.
         * @return {@code this}
         */
        public Builder sslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        /**
         * Sets the {@link SslVerifyMode}.
         *
         * @param sslVerifyMode the {@link SslVerifyMode}.
         * @return {@code this}
         */
        public Builder sslVerifyMode(SslVerifyMode sslVerifyMode) {
            this.sslVerifyMode = sslVerifyMode;
            return this;
        }

        /**
         * The SSL/TLS protocol to be used to initialize {@link SSLContext}.
         * @param protocol the ssl/tls protocol
         * @return {@code this}
         */
        public Builder sslProtocol(String protocol) {
            this.sslProtocol = protocol;
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

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        if (sslVerifyMode == SslVerifyMode.FULL) {
            this.sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        } else if (sslVerifyMode == SslVerifyMode.CA) {
            this.sslParameters.setEndpointIdentificationAlgorithm("");
        } else if (sslVerifyMode == SslVerifyMode.INSECURE) {
            trustManagers = new TrustManager[] { INSECURE_TRUST_MANAGER };
        }

        if (keystoreResource != null) {

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            try (InputStream keystoreStream = keystoreResource.get()) {
                keyStore.load(keystoreStream, keystorePassword);
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            keyManagerFactory.init(keyStore, keystorePassword);
            keyManagers = keyManagerFactory.getKeyManagers();
        }

        if (trustManagers == null && truststoreResource != null) {

            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            try (InputStream truststoreStream = truststoreResource.get()) {
                trustStore.load(truststoreStream, truststorePassword);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        SSLContext sslContext = SSLContext.getInstance(sslProtocol);
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    /**
     * {@link #createSslContext()} must be called before this.
     * @return {@link SSLParameters}
     */
    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    private static char[] getPassword(char[] chars) {
        return chars != null ? Arrays.copyOf(chars, chars.length) : null;
    }

    /**
     * Assert that {@code file} {@link File#exists() exists}.
     *
     * @param keyword file recognizer
     * @param file
     * @throws IllegalArgumentException if the file doesn't exist
     */
    public static void assertFile(String keyword, File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("%s file %s does not exist", keyword, file));
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException(String.format("%s file %s is not a file", keyword, file));
        }
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

    private static final X509Certificate[] EMPTY_X509_CERTIFICATES = {};

    private static final TrustManager INSECURE_TRUST_MANAGER = new X509ExtendedTrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) {
            if (logger.isDebugEnabled()) {
                logger.debug("Accepting a client certificate: " + chain[0].getSubjectDN());
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) {
            if (logger.isDebugEnabled()) {
                logger.debug("Accepting a server certificate: " + chain[0].getSubjectDN());
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            checkClientTrusted(chain, s);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            checkClientTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            checkServerTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            checkServerTrusted(chain, s);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_X509_CERTIFICATES;
        }
    };

}
