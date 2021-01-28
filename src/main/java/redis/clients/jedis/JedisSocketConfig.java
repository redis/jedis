package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public interface JedisSocketConfig {

  int getConnectionTimeout();

  int getSoTimeout();

  boolean isSsl();

  SSLSocketFactory getSslSocketFactory();

  SSLParameters getSslParameters();

  HostnameVerifier getHostnameVerifier();

  HostAndPortMapper getHostAndPortMapper();
}
