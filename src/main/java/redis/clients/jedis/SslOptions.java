package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

/**
 * Options to configure SSL options for the connections kept to Redis servers.
 */
public class SslOptions {
    private final SSLSocketFactory sslSocketFactory;
    private final SSLParameters sslParameters;
    private final HostnameVerifier hostnameVerifier;

    public SslOptions(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public static SslOptionsBuilder builder(){
        return new SslOptionsBuilder();
    }

    public static SslOptions create(){
        return builder().build();
    }

    public static class SslOptionsBuilder {
        private SSLSocketFactory sslSocketFactory;
        private SSLParameters sslParameters;
        private HostnameVerifier hostnameVerifier;

        private SslOptionsBuilder() {
        }

        public SslOptionsBuilder withSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public SslOptionsBuilder withSslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        public SslOptionsBuilder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public SslOptions build() {
            return new SslOptions(sslSocketFactory, sslParameters, hostnameVerifier);
        }
    }
}
