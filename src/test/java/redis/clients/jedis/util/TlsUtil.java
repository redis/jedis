package redis.clients.jedis.util;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertTrue;

public class TlsUtil {

    private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";

    private static String originalTrustStore;
    private static String originalTrustStoreType;
    private static String originalTrustStorePassword;

    private static final String TRUST_STORE_TYPE = "JCEKS";
    private static final String CERTIFICATE_TYPE = "X.509";

    private static final String TEST_WORK_FOLDER = System.getenv().getOrDefault("TEST_WORK_FOLDER", "./redis-env/");
    private static final String TEST_TRUSTSTORE = System.getenv().getOrDefault("TEST_TRUSTSTORE", "truststore.jceks");
    private static final String TEST_CA_CERT = System.getenv().getOrDefault("TEST_CA_CERT", "work/tls/ca.crt");

    public static void setCustomTrustStore(Path customTrustStorePath, String customTrustStorePassword) {
        // Store original properties
        originalTrustStore = System.getProperty(TRUST_STORE_PROPERTY);
        originalTrustStorePassword = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
        originalTrustStoreType = System.getProperty(TRUST_STORE_TYPE_PROPERTY, "JKS");
        // Set new properties for the custom truststore
        System.setProperty(TRUST_STORE_PROPERTY, customTrustStorePath.toAbsolutePath().toString());
        System.setProperty(TRUST_STORE_TYPE_PROPERTY, TRUST_STORE_TYPE);
        if (customTrustStorePassword != null) {
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, customTrustStorePassword);
        } else {
            System.clearProperty(TRUST_STORE_PASSWORD_PROPERTY);
        }
    }

    public static void restoreOriginalTrustStore() {
        // Restore original properties
        if (originalTrustStore != null) {
            System.setProperty(TRUST_STORE_PROPERTY, originalTrustStore);
        } else {
            System.clearProperty(TRUST_STORE_PROPERTY);
        }

        if ( originalTrustStoreType != null) {
            System.setProperty(TRUST_STORE_TYPE_PROPERTY, originalTrustStoreType);
        } else {
            System.clearProperty(TRUST_STORE_TYPE_PROPERTY);
        }

        if (originalTrustStorePassword != null) {
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, originalTrustStorePassword);
        } else {
            System.clearProperty(TRUST_STORE_PASSWORD_PROPERTY);
        }
    }

    public static Path envCa(String env) {
        if (TestEnvUtil.isContainerEnv()) {
            return Paths.get(TEST_WORK_FOLDER, env, TEST_CA_CERT);
        } else {
            return Paths.get("src/test/resources/private.crt");
        }
    }

    public static Path envTruststore(String env) {

        if (TestEnvUtil.isContainerEnv()) {
            return Paths.get(TEST_WORK_FOLDER, env  + '-' + TEST_TRUSTSTORE);
        } else {
            return Paths.get("src/test/resources/truststore.jceks");
        }
    }

    /**
     * Loads the CA certificate from the provided file path.
     *
     * @param caCertPath Path to the CA certificate file (ca.crt).
     * @return Loaded X509Certificate.
     * @throws Exception If there's an error reading the certificate.
     */
    public static X509Certificate loadCACertificate(String caCertPath) {
        File caCertFile = new File(caCertPath);
        try (FileInputStream fis = new FileInputStream(caCertFile)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
            return (X509Certificate) certificateFactory.generateCertificate(fis);
        } catch (CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path createAndSaveEnvTruststore(String env, String truststorePassword) {
        String caPath = envCa(env).toAbsolutePath().toString();
        String trustStorePath = envTruststore(env).toAbsolutePath().toString();
        return createAndSaveTruststore(caPath, trustStorePath, truststorePassword);
    }

    /**
     * Creates a truststore with the given CA certificate.
     *
     * @param caCertPath Path to the CA certificate file (ca.crt).
     * @return A KeyStore object containing the CA certificate.
     * @throws Exception If there's an error creating the truststore.
     */
    public static KeyStore createTruststore(String caCertPath) throws Exception {
        X509Certificate caCert = loadCACertificate(caCertPath);

        KeyStore trustStore = KeyStore.getInstance(TRUST_STORE_TYPE);
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-cert", caCert);

        return trustStore;
    }

    /**
     * Creates a truststore with the given CA certificate and saves it to the specified path.
     *
     * @param caCertPath         Path to the CA certificate file (ca.crt).
     * @param truststorePath     Path to save the generated truststore.
     * @param truststorePassword Password for the truststore.
     * @return Path to the saved truststore file.
     * @throws Exception If there's an error creating or saving the truststore.
     */
    public static Path createAndSaveTruststore(String caCertPath, String truststorePath, String truststorePassword) {
        try {
            KeyStore trustStore = createTruststore(caCertPath);

            // Save the truststore to the specified path
            try (FileOutputStream fos = new FileOutputStream(truststorePath)) {
                trustStore.store(fos, truststorePassword.toCharArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to save truststore to " + truststorePath + ": " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create and save truststore: " + e.getMessage(), e);
        }

        return Paths.get(truststorePath);
    }


    /**
     * Creates an SSLSocketFactory that trusts all certificates in truststore.jceks.
     * for given test environment
     */
    public static SSLSocketFactory sslSocketFactoryForEnv(String envName){
       return sslSocketFactory(envCa(envName));
    }

    /**
     * Returns SSLSocketFactory configured with Truststore containing provided CA cert
     */
    public static SSLSocketFactory sslSocketFactory(Path caCertPath){

        KeyStore truststore = null;
        try {
            truststore = createTruststore(caCertPath.toAbsolutePath().toString());


            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(truststore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise SslSocketFactory for " + caCertPath, e);
        }

    }

    /**
     * Creates an SSLSocketFactory with a trust manager that does not trust any certificates.
     */
    public static SSLSocketFactory createTrustNoOneSslSocketFactory() throws Exception {
        TrustManager[] unTrustManagers = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                throw new RuntimeException(new InvalidAlgorithmParameterException());
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                throw new RuntimeException(new InvalidAlgorithmParameterException());
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, unTrustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    public static class LocalhostVerifier extends BasicHostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            if (hostname.equals("127.0.0.1")) {
                hostname = "localhost";
            }
            return super.verify(hostname, session);
        }
    }

    /**
     * Very basic hostname verifier implementation for testing. NOT recommended for production.
     */
    public static class BasicHostnameVerifier implements HostnameVerifier {

        private static final String COMMON_NAME_RDN_PREFIX = "CN=";

        @Override
        public boolean verify(String hostname, SSLSession session) {
            X509Certificate peerCertificate;
            try {
                peerCertificate = (X509Certificate) session.getPeerCertificates()[0];
            } catch (SSLPeerUnverifiedException e) {
                throw new IllegalStateException("The session does not contain a peer X.509 certificate.", e);
            }
            String peerCertificateCN = getCommonName(peerCertificate);
            return hostname.equals(peerCertificateCN);
        }

        private String getCommonName(X509Certificate peerCertificate) {
            String subjectDN = peerCertificate.getSubjectDN().getName();
            String[] dnComponents = subjectDN.split(",");
            for (String dnComponent : dnComponents) {
                dnComponent = dnComponent.trim();
                if (dnComponent.startsWith(COMMON_NAME_RDN_PREFIX)) {
                    return dnComponent.substring(COMMON_NAME_RDN_PREFIX.length());
                }
            }
            throw new IllegalArgumentException("The certificate has no common name.");
        }
    }

    public static SSLSocketFactory createTrustAllSslSocketFactory()  {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // Trust all clients
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // Trust all servers
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to create a trust-all SSL socket factory", e);
        }
    }
}