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

  private String host = Protocol.DEFAULT_HOST;
  private int port = Protocol.DEFAULT_PORT;
  private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  private int soTimeout = Protocol.DEFAULT_TIMEOUT;
  private boolean ssl = false;
  private SSLSocketFactory sslSocketFactory = null;
  private SSLParameters sslParameters = null;
  private HostnameVerifier hostnameVerifier = null;
  private HostAndPortMapper hostAndPortMapper = null;

  public DefaultJedisSocketFactory() {
  }

  @Deprecated
  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int soTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    setHost(host);
    setPort(port);
    setConnectionTimeout(connectionTimeout);
    setSoTimeout(soTimeout);
    setSsl(ssl);
    setSslSocketFactory(sslSocketFactory);
    setSslParameters(sslParameters);
    setHostnameVerifier(hostnameVerifier);
  }

  public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
    setHostAndPort(hostAndPort);
    if (config != null) {
      setConnectionTimeout(config.getConnectionTimeout());
      setSoTimeout(config.getSoTimeout());
      setSsl(config.isSsl());
      setSslSocketFactory(config.getSslSocketFactory());
      setSslParameters(config.getSslParameters());
      setHostnameVerifier(config.getHostnameVerifier());
      setHostAndPortMapper(config.getHostAndPortMapper());
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

  public HostAndPort getSocketHostAndPort() {
    HostAndPortMapper mapper = getHostAndPortMapper();
    HostAndPort hostAndPort = getHostAndPort();
    if (mapper != null) {
      HostAndPort mapped = mapper.getHostAndPort(hostAndPort);
      if (mapped != null) return mapped;
    }
    return hostAndPort;
  }

  public HostAndPort getHostAndPort() {
    return new HostAndPort(this.host, this.port);
  }

  public void setHostAndPort(HostAndPort hostPort) {
    this.host = hostPort.getHost();
    this.port = hostPort.getPort();
  }

  @Override
  public String getDescription() {
    return host + ":" + port;
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
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
    return this.soTimeout;
  }

  @Override
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
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
