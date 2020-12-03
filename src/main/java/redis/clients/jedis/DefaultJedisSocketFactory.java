package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DefaultJedisSocketFactory implements JedisSocketFactory {

  private String host;
  private int port;
  private int connectionTimeout;
  private int soTimeout;
  private boolean ssl;
  private SSLSocketFactory sslSocketFactory;
  private SSLParameters sslParameters;
  private HostnameVerifier hostnameVerifier;

  public DefaultJedisSocketFactory(String host, int port, int connectionTimeout, int soTimeout,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this.host = host;
    this.port = port;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
  }

  @Override
  public Socket createSocket() throws IOException {
    Socket socket = null;
    try {
      socket = new Socket();
      // ->@wjw_add
      socket.setReuseAddress(true);
      socket.setKeepAlive(true); // Will monitor the TCP connection is
      // valid
      socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to
      // ensure timely delivery of data
      socket.setSoLinger(true, 0); // Control calls close () method,
      // the underlying socket is closed
      // immediately
      // <-@wjw_add

      socket.connect(new InetSocketAddress(getHost(), getPort()), getConnectionTimeout());
      socket.setSoTimeout(getSoTimeout());

      if (ssl) {
        if (null == sslSocketFactory) {
          sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        socket = sslSocketFactory.createSocket(socket, getHost(), getPort(), true);
        if (null != sslParameters) {
          ((SSLSocket) socket).setSSLParameters(sslParameters);
        }
        if ((null != hostnameVerifier)
            && (!hostnameVerifier.verify(getHost(), ((SSLSocket) socket).getSession()))) {
          String message = String.format(
            "The connection to '%s' failed ssl/tls hostname verification.", getHost());
          throw new JedisConnectionException(message);
        }
      }
      return socket;
    } catch (Exception ex) {
      if (socket != null) {
        socket.close();
      }
      throw ex;
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
    this.host = host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  public int getSoTimeout() {
    return soTimeout;
  }

  @Override
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }
}
