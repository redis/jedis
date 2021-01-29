package redis.clients.jedis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

public class DefaultJedisSocketFactory implements JedisSocketFactory {

  private HostAndPort hostPort; // TODO: should be final
  private final JedisSocketConfig config;

  @Deprecated
  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int soTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(new HostAndPort(host, port),
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

  public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisSocketConfig socketConfig) {
    this.hostPort = hostAndPort;
    this.config = socketConfig != null ? socketConfig : DefaultJedisSocketConfig.DEFAULT_SOCKET_CONFIG;
  }

  @Override
  public Socket createSocket() throws JedisConnectionException {
    Socket socket = null;
    try {
      socket = new Socket();
      // ->@wjw_add
      socket.setReuseAddress(true);
      socket.setKeepAlive(true); // Will monitor the TCP connection is valid
      socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to ensure timely delivery of data
      socket.setSoLinger(true, 0); // Control calls close () method, the underlying socket is closed immediately
      // <-@wjw_add

      HostAndPort hostAndPort = getSocketHostAndPort();
      socket.connect(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()), getConnectionTimeout());
      socket.setSoTimeout(getSoTimeout());

      if (config.isSsl()) {
        SSLSocketFactory sslSocketFactory = config.getSslSocketFactory();
        if (null == sslSocketFactory) {
          sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        socket = sslSocketFactory.createSocket(socket, hostAndPort.getHost(), hostAndPort.getPort(), true);

        SSLParameters sslParameters = config.getSslParameters();
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

    } catch (IOException ex) {

      IOUtils.closeQuietly(socket);

      throw new JedisConnectionException("Failed to create socket.", ex);
    }
  }

  public HostAndPort getHostAndPort() {
    return this.hostPort;
  }

  public HostAndPort getSocketHostAndPort() {
    HostAndPortMapper mapper = config.getHostAndPortMapper();
    HostAndPort hostAndPort = getHostAndPort();
    if (mapper != null) {
      HostAndPort mapped = mapper.getHostAndPort(hostAndPort);
      if (mapped != null) return mapped;
    }
    return hostAndPort;
  }

  @Override
  public String getDescription() {
    return this.hostPort.toString();
  }

  @Override
  public String getHost() {
    return this.hostPort.getHost();
  }

  /**
   * @param host
   * @deprecated This method will be removed in future.
   */
  @Override
  @Deprecated
  public void setHost(String host) {
    this.hostPort = new HostAndPort(host, this.hostPort.getPort());
  }

  @Override
  public int getPort() {
    return this.hostPort.getPort();
  }

  /**
   * @param port
   * @deprecated This method will be removed in future.
   */
  @Override
  @Deprecated
  public void setPort(int port) {
    this.hostPort = new HostAndPort(this.hostPort.getHost(), port);
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
