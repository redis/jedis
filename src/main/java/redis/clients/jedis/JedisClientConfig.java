package redis.clients.jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public interface JedisClientConfig {

  /**
   * @return Connection timeout in milliseconds
   */
  default int getConnectionTimeoutMillis() {
    return Protocol.DEFAULT_TIMEOUT;
  }

  /**
   * @return Socket timeout in milliseconds
   */
  default int getSocketTimeoutMillis() {
    return Protocol.DEFAULT_TIMEOUT;
  }

  /**
   * @return Socket timeout (in milliseconds) to use during blocking operation. Default is '0',
   * which means to block forever.
   */
  default int getBlockingSocketTimeoutMillis() {
    return 0;
  }

  /**
   * @return Redis ACL user
   */
  default String getUser() {
    return null;
  }

  default String getPassword() {
    return null;
  }

  default void updatePassword(String password) {
  }

  default int getDatabase() {
    return Protocol.DEFAULT_DATABASE;
  }

  default String getClientName() {
    return null;
  }

  /**
   * @return <code>true</code> - to create a TLS connection. <code>false</code> - otherwise.
   */
  default boolean isSsl() {
    return false;
  }

  default SSLSocketFactory getSslSocketFactory() {
    return null;
  }

  default SSLParameters getSslParameters() {
    return null;
  }

  default HostnameVerifier getHostnameVerifier() {
    return null;
  }

  default HostAndPortMapper getHostAndPortMapper() {
    return null;
  }

}
