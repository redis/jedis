package redis.clients.jedis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

public class DefaultJedisSocketFactory implements JedisSocketFactory {

  protected static final HostAndPort DEFAULT_HOST_AND_PORT = new HostAndPort(Protocol.DEFAULT_HOST,
      Protocol.DEFAULT_PORT);

  private final AtomicReference<HostAndPort> hostAndPort = new AtomicReference<>(DEFAULT_HOST_AND_PORT);
  private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  private int socketTimeout = Protocol.DEFAULT_TIMEOUT;
  private boolean ssl = false;
  private SSLSocketFactory sslSocketFactory = null;
  private SSLParameters sslParameters = null;
  private HostnameVerifier hostnameVerifier = null;
  private HostAndPortMapper hostAndPortMapper = null;

  public DefaultJedisSocketFactory() {
  }

  public DefaultJedisSocketFactory(HostAndPort hostAndPort) {
    this(hostAndPort, null);
  }

  public DefaultJedisSocketFactory(JedisClientConfig config) {
    this(null, config);
  }

  @Deprecated
  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int socketTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this.hostAndPort.set(new HostAndPort(host, port));
    this.connectionTimeout = connectionTimeout;
    this.socketTimeout = socketTimeout;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
    if (hostAndPort != null) {
      this.hostAndPort.set(hostAndPort);
    }
    if (config != null) {
      this.connectionTimeout = config.getConnectionTimeoutMillis();
      this.socketTimeout = config.getSocketTimeoutMillis();
      this.ssl = config.isSsl();
      this.sslSocketFactory = config.getSslSocketFactory();
      this.sslParameters = config.getSslParameters();
      this.hostnameVerifier = config.getHostnameVerifier();
      this.hostAndPortMapper = config.getHostAndPortMapper();
    }
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

      if (ssl) {
        SSLSocketFactory sslSocketFactory = getSslSocketFactory();
        if (null == sslSocketFactory) {
          sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        socket = sslSocketFactory.createSocket(socket, hostAndPort.getHost(), hostAndPort.getPort(), true);

        SSLParameters sslParameters = getSslParameters();
        if (null != sslParameters) {
          ((SSLSocket) socket).setSSLParameters(sslParameters);
        }

        HostnameVerifier hostnameVerifier = getHostnameVerifier();
        if (null != hostnameVerifier
            && !hostnameVerifier.verify(hostAndPort.getHost(), ((SSLSocket) socket).getSession())) {
          String message = String.format(
            "The connection to '%s' failed ssl/tls hostname verification.", hostAndPort.getHost());
          throw new JedisConnectionException(message);
        }
      }

      return socket;

    } catch (IOException ex) {

      IOUtils.closeQuietly(socket);

      throw new JedisConnectionException("Failed to create socket.", ex);
    }
  }

  @Override
  public void updateHostAndPort(HostAndPort hostAndPort) {
    this.hostAndPort.set(hostAndPort);
  }

  public HostAndPort getSocketHostAndPort() {
    HostAndPortMapper mapper = getHostAndPortMapper();
    HostAndPort hostAndPort = getHostAndPort();
    if (mapper != null) {
      HostAndPort mapped = mapper.getHostAndPort(hostAndPort);
      if (mapped != null) {
        return mapped;
      }
    }
    return hostAndPort;
  }

  public HostAndPort getHostAndPort() {
    return this.hostAndPort.get();
  }

  public void setHostAndPort(HostAndPort hostAndPort) {
    this.hostAndPort.set(hostAndPort);
  }

  @Override
  public String getDescription() {
    return this.hostAndPort.toString();
  }

  @Override
  public String getHost() {
    return this.hostAndPort.get().getHost();
  }

  @Override
  public void setHost(String host) {
    this.hostAndPort.set(new HostAndPort(host, this.hostAndPort.get().getPort()));
  }

  @Override
  public int getPort() {
    return this.hostAndPort.get().getPort();
  }

  @Override
  public void setPort(int port) {
    this.hostAndPort.set(new HostAndPort(this.hostAndPort.get().getHost(), port));
  }

  @Override
  public int getConnectionTimeout() {
    return this.connectionTimeout;
  }

  @Override
  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  public int getSoTimeout() {
    return this.socketTimeout;
  }

  @Override
  public void setSoTimeout(int soTimeout) {
    this.socketTimeout = soTimeout;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }

  public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
  }

  public SSLParameters getSslParameters() {
    return sslParameters;
  }

  public void setSslParameters(SSLParameters sslParameters) {
    this.sslParameters = sslParameters;
  }

  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }

  public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
  }

  public HostAndPortMapper getHostAndPortMapper() {
    return hostAndPortMapper;
  }

  public void setHostAndPortMapper(HostAndPortMapper hostAndPortMapper) {
    this.hostAndPortMapper = hostAndPortMapper;
  }
}
