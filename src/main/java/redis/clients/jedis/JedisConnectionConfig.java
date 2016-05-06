package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public class JedisConnectionConfig {
  private String host;
  private int port;
  private String password;
  private int dbIndex;
  private boolean ssl;
  private final int connectTimeout;
  private final int soTimeout;
  private final int subscribeSoTimeout;
  private final SSLSocketFactory sslSocketFactory;
  private final SSLParameters sslParameters;
  private final String clientName;
  private final HostnameVerifier hostnameVerifier;

  public JedisConnectionConfig(String host, int port, String password, int dbIndex, boolean ssl,
      int connectTimeout, int soTimeout, int subscribeSoTimeout, SSLSocketFactory sslSocketFactory,
      SSLParameters sslParameters, String clientName, HostnameVerifier hostnameVerifier) {
    this.host = host;
    this.port = port;
    this.password = password;
    this.dbIndex = dbIndex;
    this.ssl = ssl;
    this.connectTimeout = connectTimeout;
    this.soTimeout = soTimeout;
    this.subscribeSoTimeout = subscribeSoTimeout;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.clientName = clientName;
    this.hostnameVerifier = hostnameVerifier;
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

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPassword() {
    return password;
  }

  public int getDbIndex() {
    return dbIndex;
  }

  public boolean isSsl() {
    return ssl;
  }
}
