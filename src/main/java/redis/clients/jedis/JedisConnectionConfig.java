package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

public class JedisConnectionConfig {
    private final URI uri;
    private final int connectTimeout;
    private final int soTimeout;
    private final int subscribeSoTimeout;
    private final SSLSocketFactory sslSocketFactory;
    private final SSLParameters sslParameters;
    private final String clientName;
    private final HostnameVerifier hostnameVerifier;

    public JedisConnectionConfig(URI uri, int connectTimeout, int soTimeout, int subscribeSoTimeout, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, String clientName, HostnameVerifier hostnameVerifier) {
        this.uri = uri;
        this.connectTimeout = connectTimeout;
        this.soTimeout = soTimeout;
        this.subscribeSoTimeout = subscribeSoTimeout;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.clientName = clientName;
        this.hostnameVerifier = hostnameVerifier;
    }

    public URI getUri() {
        return uri;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public int getSubscribeSoTimeout() {
        return subscribeSoTimeout;
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

    public String getClientName() {
        return clientName;
    }
}
