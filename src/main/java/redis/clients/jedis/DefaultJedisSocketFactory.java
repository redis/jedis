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

  private final HostAndPort hostPort;
  private final JedisClientConfig config;

  @Deprecated
  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int soTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(new HostAndPort(host, port),
        DefaultJedisClientConfig.builder()
            .withConnectionTimeout(connectionTimeout)
            .withSoTimeout(soTimeout)
            .withSsl(ssl)
            .withSslSocketFactory(sslSocketFactory)
            .withSslParameters(sslParameters)
            .withHostnameVerifier(hostnameVerifier)
            .build()
    );
  }

  public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
    this.hostPort = hostAndPort;
    this.config = config != null ? config : DefaultJedisClientConfig.builder().build();
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

  @Override
  public void setHost(String host) {
    // throw exception?
  }

  @Override
  public int getPort() {
    return this.hostPort.getPort();
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

  @Override
  public int getInfiniteSoTimeout() {
    return config.getInfiniteSoTimeout();
  }
}
