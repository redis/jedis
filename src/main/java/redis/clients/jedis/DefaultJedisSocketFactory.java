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

  protected static final HostAndPort DEFAULT_HOST_AND_PORT = new HostAndPort(Protocol.DEFAULT_HOST,
      Protocol.DEFAULT_PORT);

  private volatile HostAndPort hostAndPort = DEFAULT_HOST_AND_PORT;
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
    this.hostAndPort = new HostAndPort(host, port);
    this.connectionTimeout = connectionTimeout;
    this.socketTimeout = socketTimeout;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
    if (hostAndPort != null) {
      this.hostAndPort = hostAndPort;
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
      socket.setReuseAddress(true);
      socket.setKeepAlive(true); // Will monitor the TCP connection is valid
      socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to ensure timely delivery of data
      socket.setSoLinger(true, 0); // Control calls close () method, the underlying socket is closed immediately

      HostAndPort _hostAndPort = getSocketHostAndPort();
      socket.connect(new InetSocketAddress(_hostAndPort.getHost(), _hostAndPort.getPort()), connectionTimeout);
      socket.setSoTimeout(socketTimeout);

      if (ssl) {
        SSLSocketFactory _sslSocketFactory = this.sslSocketFactory;
        if (null == _sslSocketFactory) {
          _sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        socket = _sslSocketFactory.createSocket(socket, _hostAndPort.getHost(), _hostAndPort.getPort(), true);

        if (null != sslParameters) {
          ((SSLSocket) socket).setSSLParameters(sslParameters);
        }

        if (null != hostnameVerifier
            && !hostnameVerifier.verify(_hostAndPort.getHost(), ((SSLSocket) socket).getSession())) {
          String message = String.format(
            "The connection to '%s' failed ssl/tls hostname verification.", _hostAndPort.getHost());
          throw new JedisConnectionException(message);
        }
      }

      return socket;

    } catch (IOException ex) {

      IOUtils.closeQuietly(socket);

      throw new JedisConnectionException("Failed to create socket.", ex);
    }
  }

  public void updateHostAndPort(HostAndPort hostAndPort) {
    this.hostAndPort = hostAndPort;
  }

  protected HostAndPort getSocketHostAndPort() {
    HostAndPortMapper mapper = hostAndPortMapper;
    HostAndPort hap = this.hostAndPort;
    if (mapper != null) {
      HostAndPort mapped = mapper.getHostAndPort(hap);
      if (mapped != null) {
        return mapped;
      }
    }
    return hap;
  }

  @Override
  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  @Override
  public int getSocketTimeout() {
    return socketTimeout;
  }

  @Override
  public String toString() {
    return "DefaultJedisSocketFactory{" + hostAndPort.toString() + "}";
  }
}
