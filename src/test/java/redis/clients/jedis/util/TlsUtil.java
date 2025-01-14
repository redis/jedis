package redis.clients.jedis.util;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TlsUtil {

    private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";

    private static String originalTrustStore;
    private static String originalTrustStoreType;
    private static String originalTrustStorePassword;

    private static final String TRUST_STORE_TYPE = "JCEKS";
    private static final String CERTIFICATE_TYPE = "X.509";

    private static final String TEST_WORK_FOLDER = System.getenv().getOrDefault("TEST_WORK_FOLDER", "/tmp/redis-env-work");
    private static final String TEST_TRUSTSTORE = System.getenv().getOrDefault("TEST_TRUSTSTORE", "truststore.jceks");
    private static final String TEST_CA_CERT = System.getenv().getOrDefault("TEST_CA_CERT", "ca.crt");
    private static final String TEST_SERVER_CERT = System.getenv().getOrDefault("TEST_SERVER_CERT", "redis.crt");

    public static void setCustomTrustStore(Path customTrustStorePath, String customTrustStorePassword) {
        // Store original properties
        originalTrustStore = System.getProperty(TRUST_STORE_PROPERTY);
        originalTrustStorePassword = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
        originalTrustStoreType = System.getProperty(TRUST_STORE_TYPE_PROPERTY);
        // Set new properties for the custom truststore
        System.setProperty(TRUST_STORE_PROPERTY, customTrustStorePath.toAbsolutePath().toString());
        System.setProperty(TRUST_STORE_TYPE_PROPERTY, TRUST_STORE_TYPE);
        if (customTrustStorePassword != null) {
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, customTrustStorePassword);
        } else {
            System.clearProperty(TRUST_STORE_PASSWORD_PROPERTY);
        }

        reinitializeDefaultSSLContext();
    }

    public static void reinitializeDefaultSSLContext(){
        String trustStorePath = System.getProperty(TRUST_STORE_PROPERTY);
        String trustStorePassword =  System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
        String trustStoreType = System.getProperty(TRUST_STORE_TYPE_PROPERTY, KeyStore.getDefaultType());
        // Load the new truststore
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(trustStoreType);
            try (java.io.FileInputStream trustStoreStream = new java.io.FileInputStream(trustStorePath)) {
                trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
            } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext newSslContext = SSLContext.getInstance("TLS");
            newSslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(newSslContext);
            } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
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

    private static Path envCa(Path certLocation) {
        if (TestEnvUtil.isContainerEnv()) {
            return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_CA_CERT);
        } else {
            // Legacy test env uses same certificate & truststore for all tests
            return Paths.get("src/test/resources/private.crt");
        }
    }

    private static Path envServerCert(Path certLocation) {
        if (TestEnvUtil.isContainerEnv()) {
            return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_SERVER_CERT);
        } else {
            // Legacy test env uses same certificate & truststore for all tests
            return Paths.get("src/test/resources/private.crt");
        }
    }

    public static Path testTruststorePath(String name) {
        if (TestEnvUtil.isContainerEnv()) {
            return Paths.get(TEST_WORK_FOLDER, name  + '-' + TEST_TRUSTSTORE);
        } else {
            // Legacy test env uses same certificate & truststore for all tests
            return Paths.get("src/test/resources/truststore.jceks");
        }
    }

    public static Path createAndSaveTestTruststore(String trustStoreName, List<Path> certificateLocations, String truststorePassword) {
        List<Path> trustedCertPaths = new ArrayList<>();

        // Traverse each location in certificateLocations and add both CA and Server certificates
        for (Path location : certificateLocations) {
            trustedCertPaths.add(envCa(location).toAbsolutePath());
            trustedCertPaths.add(envServerCert(location).toAbsolutePath());
        }

        Path trustStorePath = testTruststorePath(trustStoreName).toAbsolutePath();

        return createAndSaveTruststore(trustedCertPaths, trustStorePath, truststorePassword);
    }

    /**
     * Creates an empty truststore.
     *
     * @return An empty KeyStore object.
     * @throws KeyStoreException If there's an error initializing the truststore.
     * @throws IOException        If there's an error loading the truststore.
     * @throws NoSuchAlgorithmException If the algorithm used to check the integrity of the truststore cannot be found.
     * @throws CertificateException If any of the certificates in the truststore could not be loaded.
     */
    private static KeyStore createTruststore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore trustStore = KeyStore.getInstance(TRUST_STORE_TYPE);
        trustStore.load(null, null);
        return trustStore;
    }

    /**
     * Adds a trusted certificate to the given truststore.
     *
     * @param trustStore The KeyStore object.
     * @param alias      Alias for the certificate.
     * @param certPath   Path to the certificate file.
     * @throws Exception If there's an error adding the certificate.
     */
    private static void addTrustedCertificate(KeyStore trustStore, String alias, Path certPath) throws Exception {
        X509Certificate cert = loadCertificate(certPath);
        trustStore.setCertificateEntry(alias, cert);
    }

    /**
     * Loads an X.509 certificate from the given file path.
     *
     * @param certPath Path to the certificate file.
     * @return An X509Certificate object.
     * @throws Exception If there's an error loading the certificate.
     */
    private static X509Certificate loadCertificate(Path certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath.toFile())) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(fis);
        }
    }

    /**
     * Creates a truststore, adds multiple trusted certificates, and saves it to the specified path.
     *
     * @param trustedCertPaths   List of certificate file paths to add to the truststore.
     * @param truststorePath     Path to save the generated truststore.
     * @param truststorePassword Password for the truststore.
     * @return Path to the saved truststore file.
     */
    public static Path createAndSaveTruststore(List<Path> trustedCertPaths, Path truststorePath, String truststorePassword) {
        try {
            KeyStore trustStore = createTruststore();

            for (Path certPath : trustedCertPaths) {
                addTrustedCertificate(trustStore, "trusted-cert-" + UUID.randomUUID(), certPath);
            }

            try (FileOutputStream fos = new FileOutputStream(truststorePath.toFile())) {
                trustStore.store(fos, truststorePassword.toCharArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to save truststore to " + truststorePath + ": " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create and save truststore: " + e.getMessage(), e);
        }

        return truststorePath;
    }


    /**
     * Creates an SSLSocketFactory that trusts all certificates in truststore.jceks.
     * for given test environment
     */
    public static SSLSocketFactory sslSocketFactoryForEnv(Path certLocations){
       return sslSocketFactory(envCa(certLocations));
    }

    /**
     * Returns SSLSocketFactory configured with Truststore containing provided CA cert
     */
    private static SSLSocketFactory sslSocketFactory(Path trustedCertPath) {

        KeyStore trustStore = null;
        try {
            trustStore = createTruststore();
            addTrustedCertificate(trustStore, "trusted-cert-" + UUID.randomUUID(), trustedCertPath.toAbsolutePath());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(trustStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise SslSocketFactory for " + trustedCertPath , e);
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

            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
                throw new CertificateException("Using a trust manager that does not trust any certificates for test purposes!",new InvalidAlgorithmParameterException());
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
                throw new CertificateException("Using a trust manager that does not trust any certificates for test purposes!", new InvalidAlgorithmParameterException());
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, unTrustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
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

    public static class LocalhostVerifier extends BasicHostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            if (hostname.equals("127.0.0.1")) {
                hostname = "localhost";
            }
            return super.verify(hostname, session);
        }
    }
}
