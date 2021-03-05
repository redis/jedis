package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class Connection implements Closeable {

  private static final byte[][] EMPTY_ARGS = new byte[0][];

  private boolean socketParamModified = false; // for backward compatibility
  private JedisSocketFactory socketFactory; // TODO: sould be final
  private Socket socket;
  private RedisOutputStream outputStream;
  private RedisInputStream inputStream;
  private int soTimeout = Protocol.DEFAULT_TIMEOUT;
  private int infiniteSoTimeout = 0;
  private boolean broken = false;

  public Connection() {
    this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  /**
   * @param host
   * @deprecated This constructor will be removed in future. It can be replaced with
   * {@link #Connection(java.lang.String, int)} with the host and {@link Protocol#DEFAULT_PORT}.
   */
  @Deprecated
  public Connection(final String host) {
    this(host, Protocol.DEFAULT_PORT);
  }

  public Connection(final String host, final int port) {
    this(new HostAndPort(host, port), DefaultJedisClientConfig.builder().build());
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public Connection(final String host, final int port, final boolean ssl) {
    this(new HostAndPort(host, port), DefaultJedisClientConfig.builder().withSsl(ssl).build());
  }

  /**
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public Connection(final String host, final int port, final boolean ssl,
      SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier) {
    this(new HostAndPort(host, port), DefaultJedisClientConfig.builder().withSsl(ssl)
        .withSslSocketFactory(sslSocketFactory).withSslParameters(sslParameters)
        .withHostnameVerifier(hostnameVerifier).build());
  }

  public Connection(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(new DefaultJedisSocketFactory(hostAndPort, clientConfig));
    this.soTimeout = clientConfig.getSoTimeout();
    this.infiniteSoTimeout = clientConfig.getInfiniteSoTimeout();
  }

  public Connection(final JedisSocketFactory jedisSocketFactory) {
    this.socketFactory = jedisSocketFactory;
    this.soTimeout = jedisSocketFactory.getSoTimeout();
  }

  public Socket getSocket() {
    return socket;
  }

  public int getConnectionTimeout() {
    return socketFactory.getConnectionTimeout();
  }

  public int getSoTimeout() {
    return soTimeout;
  }

  /**
   * @param connectionTimeout
   * @deprecated This method is not supported anymore and is kept for backward compatibility. It
   * will be removed in future.
   */
  @Deprecated
  public void setConnectionTimeout(int connectionTimeout) {
    socketFactory.setConnectionTimeout(connectionTimeout);
  }

  public void setSoTimeout(int soTimeout) {
    socketFactory.setSoTimeout(soTimeout);
    this.soTimeout = soTimeout;
    if (this.socket != null) {
      try {
        this.socket.setSoTimeout(soTimeout);
      } catch (SocketException ex) {
        broken = true;
        throw new JedisConnectionException(ex);
      }
    }
  }

  public void setInfiniteSoTimeout(int infiniteSoTimeout) {
    this.infiniteSoTimeout = infiniteSoTimeout;
  }

  public void setTimeoutInfinite() {
    try {
      if (!isConnected()) {
        connect();
      }
      socket.setSoTimeout(infiniteSoTimeout);
    } catch (SocketException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  public void rollbackTimeout() {
    try {
      socket.setSoTimeout(socketFactory.getSoTimeout());
    } catch (SocketException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  public void sendCommand(final ProtocolCommand cmd, final String... args) {
    final byte[][] bargs = new byte[args.length][];
    for (int i = 0; i < args.length; i++) {
      bargs[i] = SafeEncoder.encode(args[i]);
    }
    sendCommand(cmd, bargs);
  }

  public void sendCommand(final ProtocolCommand cmd) {
    sendCommand(cmd, EMPTY_ARGS);
  }

  public void sendCommand(final ProtocolCommand cmd, final byte[]... args) {
    try {
      connect();
      Protocol.sendCommand(outputStream, cmd, args);
    } catch (JedisConnectionException ex) {
      /*
       * When client send request which formed by invalid protocol, Redis send back error message
       * before close connection. We try to read it to provide reason of failure.
       */
      try {
        String errorMessage = Protocol.readErrorLineIfPossible(inputStream);
        if (errorMessage != null && errorMessage.length() > 0) {
          ex = new JedisConnectionException(errorMessage, ex.getCause());
        }
      } catch (Exception e) {
        /*
         * Catch any IOException or JedisConnectionException occurred from InputStream#read and just
         * ignore. This approach is safe because reading error message is optional and connection
         * will eventually be closed.
         */
      }
      // Any other exceptions related to connection?
      broken = true;
      throw ex;
    }
  }

  public String getHost() {
    return socketFactory.getHost();
  }

  /**
   * @param host
   * @deprecated This method will be removed in future.
   */
  @Deprecated
  public void setHost(final String host) {
    socketFactory.setHost(host);
    socketParamModified = true;
  }

  public int getPort() {
    return socketFactory.getPort();
  }

  /**
   * @param port
   * @deprecated This method will be removed in future.
   */
  @Deprecated
  public void setPort(final int port) {
    socketFactory.setPort(port);
    socketParamModified = true;
  }

  public void connect() throws JedisConnectionException {
    if (socketParamModified) { // this is only for backward compatibility
      try {
        disconnect();
      } catch(Exception e) {
        // swallow
      }
    }
    if (!isConnected()) {
      try {
        socket = socketFactory.createSocket();

        outputStream = new RedisOutputStream(socket.getOutputStream());
        inputStream = new RedisInputStream(socket.getInputStream());
      } catch (IOException ioe) {
        broken = true;
        throw new JedisConnectionException("Failed to create input/output stream", ioe);
      } catch (JedisConnectionException jce) {
        broken = true;
        throw jce;
      } finally {
        if (broken) {
          IOUtils.closeQuietly(socket);
        }
      }
    }
  }

  @Override
  public void close() {
    disconnect();
  }

  public void disconnect() {
    if (isConnected()) {
      try {
        outputStream.flush();
        socket.close();
      } catch (IOException ex) {
        broken = true;
        throw new JedisConnectionException(ex);
      } finally {
        IOUtils.closeQuietly(socket);
      }
    }
  }

  public boolean isConnected() {
    return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
        && !socket.isInputShutdown() && !socket.isOutputShutdown();
  }

  public String getStatusCodeReply() {
    flush();
    final byte[] resp = (byte[]) readProtocolWithCheckingBroken();
    if (null == resp) {
      return null;
    } else {
      return SafeEncoder.encode(resp);
    }
  }

  public String getBulkReply() {
    final byte[] result = getBinaryBulkReply();
    if (null != result) {
      return SafeEncoder.encode(result);
    } else {
      return null;
    }
  }

  public byte[] getBinaryBulkReply() {
    flush();
    return (byte[]) readProtocolWithCheckingBroken();
  }

  public Long getIntegerReply() {
    flush();
    return (Long) readProtocolWithCheckingBroken();
  }

  public List<String> getMultiBulkReply() {
    return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
  }

  @SuppressWarnings("unchecked")
  public List<byte[]> getBinaryMultiBulkReply() {
    flush();
    return (List<byte[]>) readProtocolWithCheckingBroken();
  }

  @Deprecated
  public List<Object> getRawObjectMultiBulkReply() {
    return getUnflushedObjectMultiBulkReply();
  }

  @SuppressWarnings("unchecked")
  public List<Object> getUnflushedObjectMultiBulkReply() {
    return (List<Object>) readProtocolWithCheckingBroken();
  }

  public List<Object> getObjectMultiBulkReply() {
    flush();
    return getUnflushedObjectMultiBulkReply();
  }

  @SuppressWarnings("unchecked")
  public List<Long> getIntegerMultiBulkReply() {
    flush();
    return (List<Long>) readProtocolWithCheckingBroken();
  }

  public Object getOne() {
    flush();
    return readProtocolWithCheckingBroken();
  }

  public boolean isBroken() {
    return broken;
  }

  protected void flush() {
    try {
      outputStream.flush();
    } catch (IOException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  protected Object readProtocolWithCheckingBroken() {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection");
    }

    try {
      return Protocol.read(inputStream);
    } catch (JedisConnectionException exc) {
      broken = true;
      throw exc;
    }
  }

  public List<Object> getMany(final int count) {
    flush();
    final List<Object> responses = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      try {
        responses.add(readProtocolWithCheckingBroken());
      } catch (JedisDataException e) {
        responses.add(e);
      }
    }
    return responses;
  }
}
