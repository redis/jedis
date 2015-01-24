package redis.clients.jedis;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;
import redis.clients.util.SocketChannelReader;
import redis.clients.util.SocketChannelWriter;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Connection implements Closeable {
  private String host;
  private int port = Protocol.DEFAULT_PORT;
  private Socket socket;
  private SocketChannelReader channelReader;
  private SocketChannelWriter channelWriter;
  private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  private int soTimeout = Protocol.DEFAULT_TIMEOUT;
  private SocketChannel socketChannel;

  public SocketChannel getSocketChannel() {
    return socketChannel;
  }

  private boolean broken = false;

  public Socket getSocket() {
    return socket;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getSoTimeout() {
    return soTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }

  public void setTimeoutInfinite() {
    try {
      if (!isConnected()) {
        connect();
      }
      socket.setSoTimeout(0);
    } catch (SocketException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  public void rollbackTimeout() {
    try {
      socket.setSoTimeout(soTimeout);
    } catch (SocketException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  public Connection(final String host) {
    super();
    this.host = host;
  }

  protected void flush() {
    try {
      channelWriter.flush();
    } catch (IOException e) {
      throw new JedisConnectionException(e);
    }
  }

  protected Connection sendCommand(final Command cmd, final String... args) {
    final byte[][] bargs = new byte[args.length][];
    for (int i = 0; i < args.length; i++) {
      bargs[i] = SafeEncoder.encode(args[i]);
    }
    return sendCommand(cmd, bargs);
  }

  protected Connection sendCommand(final Command cmd, final byte[]... args) {
    try {
      connect();
      Protocol.sendCommand(channelWriter, cmd, args);
      return this;
    } catch (JedisConnectionException ex) {
      // Any other exceptions related to connection?
      broken = true;
      throw ex;
    }
  }

  protected Connection sendCommand(final Command cmd) {
    try {
      connect();
      Protocol.sendCommand(channelWriter, cmd, new byte[0][]);
      return this;
    } catch (JedisConnectionException ex) {
      // Any other exceptions related to connection?
      broken = true;
      throw ex;
    }
  }

  public Connection(final String host, final int port) {
    super();
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public Connection() {

  }

  public void connect() {
    if (!isConnected()) {
      try {
        socketChannel = SocketChannel.open();
        socket = socketChannel.socket();
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

        socket.connect(new InetSocketAddress(host, port), connectionTimeout);
        socket.setSoTimeout(soTimeout);
        socketChannel.configureBlocking(true);
        channelReader = new SocketChannelReader(socket.getChannel());
        channelWriter = new SocketChannelWriter(socket.getChannel());
      } catch (IOException ex) {
        broken = true;
        throw new JedisConnectionException(ex);
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
        if (socketChannel.isConnected()) {
          socketChannel.close();
        }
        if (!socket.isClosed()) {
          socket.close();
        }
      } catch (IOException ex) {
        broken = true;
        throw new JedisConnectionException(ex);
      }
    }
  }

  public boolean isConnected() {
    return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
        && socketChannel.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown();
  }

  protected String getStatusCodeReply() {
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

  @SuppressWarnings("unchecked")
  public List<Object> getRawObjectMultiBulkReply() {
    return (List<Object>) readProtocolWithCheckingBroken();
  }

  public List<Object> getObjectMultiBulkReply() {
    flush();
    return getRawObjectMultiBulkReply();
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

  protected Object readProtocolWithCheckingBroken() {
    try {
      return Protocol.read(channelReader);
    } catch (JedisConnectionException exc) {
      broken = true;
      throw exc;
    }
  }

  public List<Object> getMany(int count) {
    flush();
    List<Object> responses = new ArrayList<Object>();
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
