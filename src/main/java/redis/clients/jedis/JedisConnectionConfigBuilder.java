package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

public class JedisConnectionConfigBuilder {
    private String clientName;
    private URI uri;
    private HostnameVerifier hostnameVerifier = null;
    private int connectTimeout = Protocol.DEFAULT_TIMEOUT;
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;
    private int subscribeSoTimeout = Protocol.DEFAULT_SUBSCRIBE_TIMEOUT;
    private SSLParameters sslParameters = null;
    private SSLSocketFactory sslSocketFactory = null;

    public JedisConnectionConfigBuilder withUri(URI uri) {
        this.uri = uri;
        return this;
    }
    public JedisConnectionConfigBuilder withConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public JedisConnectionConfigBuilder withSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
        return this;
    }

    public JedisConnectionConfigBuilder withSubscribeSoTimeout(int subscribeSoTimeout) {
        this.subscribeSoTimeout = subscribeSoTimeout;
        return this;
    }

    public JedisConnectionConfigBuilder withSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public JedisConnectionConfigBuilder withSslParameters(SSLParameters sslParameters) {
        this.sslParameters = sslParameters;
        return this;
    }

    public JedisConnectionConfigBuilder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public void withClientName(String clientName) {
        this.clientName = clientName;
    }

    public JedisConnectionConfig build() {
        return new JedisConnectionConfig(uri, connectTimeout, soTimeout, subscribeSoTimeout, sslSocketFactory, sslParameters, clientName, hostnameVerifier);
    }
}