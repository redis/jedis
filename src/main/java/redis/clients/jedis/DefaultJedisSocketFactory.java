package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DefaultJedisSocketFactory implements JedisSocketFactory {

  private final String host;
  private final int port;
  private final JedisSocketConfig config;

  @Deprecated
  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int soTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(host, port,
        DefaultJedisSocketConfig.builder()
            .withConnectionTimeout(connectionTimeout)
            .withSoTimeout(soTimeout)
            .withSsl(ssl)
            .withSslSocketFactory(sslSocketFactory)
            .withSslParameters(sslParameters)
            .withHostnameVerifier(hostnameVerifier)
            .build()
    );
  }

  public DefaultJedisSocketFactory(String host, int port, JedisSocketConfig socketConfig) {
    this.host = host;
    this.port = port;
    this.config = socketConfig != null ? socketConfig : DefaultJedisSocketConfig.builder().build();
  }

  @Override
  public Socket createSocket() throws IOException {
    Socket socket = null;
    try {
      socket = new Socket();
      // ->@wjw_add
      socket.setReuseAddress(true);
      socket.setKeepAlive(true); // Will monitor the TCP connection is valid
      socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to ensure timely delivery of data
      socket.setSoLinger(true, 0); // Control calls close () method, the underlying socket is closed immediately
      // <-@wjw_add

      socket.connect(new InetSocketAddress(getHost(), getPort()), getConnectionTimeout());
      socket.setSoTimeout(getSoTimeout());

      if (config.isSSL()) {
        SSLSocketFactory sslSocketFactory = config.getSSLSocketFactory();
        if (null == sslSocketFactory) {
          sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        socket = sslSocketFactory.createSocket(socket, getHost(), getPort(), true);

        SSLParameters sslParameters = config.getSSLParameters();
        if (null != sslParameters) {
          ((SSLSocket) socket).setSSLParameters(sslParameters);
        }

        HostnameVerifier hostnameVerifier = config.getHostnameVerifier();
        if (null != hostnameVerifier
            && !hostnameVerifier.verify(getHost(), ((SSLSocket) socket).getSession())) {
          String message = String.format(
              "The connection to '%s' failed ssl/tls hostname verification.", getHost());
          throw new JedisConnectionException(message);
        }
      }

      return socket;

    } catch (Exception ex) {

      IOUtils.closeQuietly(socket);

      if (ex instanceof JedisConnectionException) {
        throw ex;
      } else {
        throw new JedisConnectionException("Failed to create socket.", ex);
      }
    }
  }

  @Override
  public String getDescription() {
    return host + ":" + port;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public void setHost(String host) {
    // throw exception?
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setPort(int port) {
    // throw exception?
  }

  @Override
  public int getConnectionTimeout() {
    return config.getConnectionTimeout();
  }

  @Override
  public void setConnectionTimeout(int connectionTimeout) {
    // throw exception?
  }

  @Override
  public int getSoTimeout() {
    return config.getSoTimeout();
  }

  @Override
  public void setSoTimeout(int soTimeout) {
    // throw exception?
  }
}
