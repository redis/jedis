package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public interface JedisSocketConfig {

  int getConnectionTimeout();

  int getSoTimeout();

  boolean isSSL();

  SSLSocketFactory getSSLSocketFactory();

  SSLParameters getSSLParameters();

  HostnameVerifier getHostnameVerifier();

  HostAndPortMapper getHostAndPortMapper();
}
