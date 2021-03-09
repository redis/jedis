package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public interface JedisClientConfig {

  int getConnectionTimeout();

  int getSoTimeout();

  /**
   * @return Socket timeout (in milliseconds) to use during blocking operation. Default is '0',
   * which means to block forever.
   */
  int getInfiniteSoTimeout();

  String getUser();

  String getPassword();

  int getDatabase();

  String getClientName();

  boolean isSsl();

  SSLSocketFactory getSslSocketFactory();

  SSLParameters getSslParameters();

  HostnameVerifier getHostnameVerifier();

  HostAndPortMapper getHostAndPortMapper();

}
