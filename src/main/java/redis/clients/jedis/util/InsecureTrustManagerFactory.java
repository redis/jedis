/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package redis.clients.jedis.util;

import java.net.Socket;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An insecure {@link TrustManagerFactory} that trusts all X.509 certificates without any verification.
 * <p>
 * <strong>NOTE:</strong>
 * Never use this {@link TrustManagerFactory} in production.
 * It is purely for testing purposes, and thus it is very insecure.
 * </p>
 */
public final class InsecureTrustManagerFactory extends TrustManagerFactory {

    private static final Logger logger = LoggerFactory.getLogger(InsecureTrustManagerFactory.class);

    public static final TrustManagerFactory INSTANCE = new InsecureTrustManagerFactory();

    private static final X509Certificate[] EMPTY_X509_CERTIFICATES = {};

    private static final Provider PROVIDER = new Provider("", 0.0, "") {
        private static final long serialVersionUID = -2680540247105807895L;
    };

    /**
     * {@link InsecureTrustManagerFactorySpi} must have a reference to {@link InsecureTrustManagerFactory}
     * to delegate its callbacks back to {@link InsecureTrustManagerFactory}.  However, it is impossible to do so,
     * because {@link TrustManagerFactory} requires {@link TrustManagerFactorySpi} at construction time and
     * does not provide a way to access it later.
     *
     * To work around this issue, we use an ugly hack which uses a {@link ThreadLocal}.
     */
    private static final ThreadLocal<InsecureTrustManagerFactorySpi> CURRENT_SPI =
            new ThreadLocal<InsecureTrustManagerFactorySpi>() {
                @Override
                protected InsecureTrustManagerFactorySpi initialValue() {
                    return new InsecureTrustManagerFactorySpi();
                }
            };

    private static final TrustManager tm = new X509TrustManager() {
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
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_X509_CERTIFICATES;
        }
    };

    private InsecureTrustManagerFactory() {
        super(CURRENT_SPI.get(), PROVIDER, "");
        CURRENT_SPI.get().init(this);
        CURRENT_SPI.remove();
    }

    /**
     * Initializes this factory with a source of certificate authorities and related trust material.
     *
     * @see TrustManagerFactorySpi#engineInit(KeyStore)
     */
    protected void engineInit(KeyStore keyStore) throws Exception { }

    /**
     * Initializes this factory with a source of provider-specific key material.
     *
     * @see TrustManagerFactorySpi#engineInit(ManagerFactoryParameters)
     */
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception { }

    /**
     * Returns one trust manager for each type of trust material.
     *
     * @see TrustManagerFactorySpi#engineGetTrustManagers()
     */
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[] { tm };
    }

    static final class InsecureTrustManagerFactorySpi extends TrustManagerFactorySpi {

        private InsecureTrustManagerFactory parent;
        private volatile TrustManager[] trustManagers;

        void init(InsecureTrustManagerFactory parent) {
            this.parent = parent;
        }

        @Override
        protected void engineInit(KeyStore keyStore) throws KeyStoreException {
            try {
                parent.engineInit(keyStore);
            } catch (KeyStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new KeyStoreException(e);
            }
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
                throws InvalidAlgorithmParameterException {
            try {
                parent.engineInit(managerFactoryParameters);
            } catch (InvalidAlgorithmParameterException e) {
                throw e;
            } catch (Exception e) {
                throw new InvalidAlgorithmParameterException(e);
            }
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            TrustManager[] trustManagers = this.trustManagers;
            if (trustManagers == null) {
                trustManagers = parent.engineGetTrustManagers();
                wrapForJava7Plus(trustManagers);
                this.trustManagers = trustManagers;
            }
            return trustManagers.clone();
        }

        private static void wrapForJava7Plus(TrustManager[] trustManagers) {
            for (int i = 0; i < trustManagers.length; i++) {
                final TrustManager tm = trustManagers[i];
                if (tm instanceof X509TrustManager && !(tm instanceof X509ExtendedTrustManager)) {
                    trustManagers[i] = new X509TrustManagerWrapper((X509TrustManager) tm);
                }
            }
        }
    }

    static final class X509TrustManagerWrapper extends X509ExtendedTrustManager {

        private final X509TrustManager delegate;

        X509TrustManagerWrapper(X509TrustManager delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
            delegate.checkClientTrusted(chain, s);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            delegate.checkClientTrusted(chain, s);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            delegate.checkClientTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
            delegate.checkServerTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            delegate.checkServerTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            delegate.checkServerTrusted(chain, s);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

}
