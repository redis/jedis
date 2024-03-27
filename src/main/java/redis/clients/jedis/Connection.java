package redis.clients.jedis;

import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

public class Connection implements Closeable {

  private ConnectionPool memberOf;
  private RedisProtocol protocol;
  private final JedisSocketFactory socketFactory;
  private Socket socket;
  private RedisOutputStream outputStream;
  private RedisInputStream inputStream;
  private ClientSideCache clientSideCache;
  private int soTimeout = 0;
  private int infiniteSoTimeout = 0;
  private boolean broken = false;

  public Connection() {
    this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
  }

  public Connection(final String host, final int port) {
    this(new HostAndPort(host, port));
  }

  public Connection(final HostAndPort hostAndPort) {
    this(new DefaultJedisSocketFactory(hostAndPort));
  }

  public Connection(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(new DefaultJedisSocketFactory(hostAndPort, clientConfig), clientConfig);
  }

  public Connection(final JedisSocketFactory socketFactory) {
    this.socketFactory = socketFactory;
  }

  public Connection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig) {
    this.socketFactory = socketFactory;
    this.soTimeout = clientConfig.getSocketTimeoutMillis();
    this.infiniteSoTimeout = clientConfig.getBlockingSocketTimeoutMillis();
    initializeFromClientConfig(clientConfig);
  }

  @Experimental
  public Connection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig,
      ClientSideCache clientSideCache) {
    this.socketFactory = socketFactory;
    this.soTimeout = clientConfig.getSocketTimeoutMillis();
    this.infiniteSoTimeout = clientConfig.getBlockingSocketTimeoutMillis();
    initializeFromClientConfig(clientConfig);
    initializeClientSideCache(clientSideCache);
  }

  @Override
  public String toString() {
    return "Connection{" + socketFactory + "}";
  }

  public final RedisProtocol getRedisProtocol() {
    return protocol;
  }

  public final void setHandlingPool(final ConnectionPool pool) {
    this.memberOf = pool;
  }

  final HostAndPort getHostAndPort() {
    return ((DefaultJedisSocketFactory) socketFactory).getHostAndPort();
  }

  public int getSoTimeout() {
    return soTimeout;
  }

  public void setSoTimeout(int soTimeout) {
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
      socket.setSoTimeout(this.soTimeout);
    } catch (SocketException ex) {
      broken = true;
      throw new JedisConnectionException(ex);
    }
  }

  public Object executeCommand(final ProtocolCommand cmd) {
    return executeCommand(new CommandArguments(cmd));
  }

  public Object executeCommand(final CommandArguments args) {
    sendCommand(args);
    return getOne();
  }

  public <T> T executeCommand(final CommandObject<T> commandObject) {
    final CommandArguments args = commandObject.getArguments();
    sendCommand(args);
    if (!args.isBlocking()) {
      return commandObject.getBuilder().build(getOne());
    } else {
      try {
        setTimeoutInfinite();
        return commandObject.getBuilder().build(getOne());
      } finally {
        rollbackTimeout();
      }
    }
  }

  public void sendCommand(final ProtocolCommand cmd) {
    sendCommand(new CommandArguments(cmd));
  }

  public void sendCommand(final ProtocolCommand cmd, Rawable keyword) {
    sendCommand(new CommandArguments(cmd).add(keyword));
  }

  public void sendCommand(final ProtocolCommand cmd, final String... args) {
    sendCommand(new CommandArguments(cmd).addObjects((Object[]) args));
  }

  public void sendCommand(final ProtocolCommand cmd, final byte[]... args) {
    sendCommand(new CommandArguments(cmd).addObjects((Object[]) args));
  }

  public void sendCommand(final CommandArguments args) {
    try {
      connect();
      Protocol.sendCommand(outputStream, args);
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

  public void connect() throws JedisConnectionException {
    if (!isConnected()) {
      try {
        socket = socketFactory.createSocket();
        soTimeout = socket.getSoTimeout(); //?

        outputStream = new RedisOutputStream(socket.getOutputStream());
        inputStream = new RedisInputStream(socket.getInputStream());

        broken = false; // unset broken status when connection is (re)initialized

      } catch (JedisConnectionException jce) {

        setBroken();
        throw jce;

      } catch (IOException ioe) {

        setBroken();
        throw new JedisConnectionException("Failed to create input/output stream", ioe);

      } finally {

        if (broken) {
          IOUtils.closeQuietly(socket);
        }
      }
    }
  }

  @Override
  public void close() {
    if (this.memberOf != null) {
      ConnectionPool pool = this.memberOf;
      this.memberOf = null;
      if (isBroken()) {
        pool.returnBrokenResource(this);
      } else {
        pool.returnResource(this);
      }
    } else {
      disconnect();
    }
  }

  /**
   * Close the socket and disconnect the server.
   */
  public void disconnect() {
    if (isConnected()) {
      try {
        outputStream.flush();
        socket.close();
      } catch (IOException ex) {
        throw new JedisConnectionException(ex);
      } finally {
        IOUtils.closeQuietly(socket);
        setBroken();
      }
    }
  }

  public boolean isConnected() {
    return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
        && !socket.isInputShutdown() && !socket.isOutputShutdown();
  }

  public boolean isBroken() {
    return broken;
  }

  public void setBroken() {
    broken = true;
  }

  public String getStatusCodeReply() {
    flush();
    final byte[] resp = (byte[]) readProtocolWithCheckingBroken();
    if (null == resp) {
      return null;
    } else {
      return encode(resp);
    }
  }

  public String getBulkReply() {
    final byte[] result = getBinaryBulkReply();
    if (null != result) {
      return encode(result);
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

  /**
   * @deprecated Use {@link Connection#getUnflushedObject()}.
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public List<Object> getUnflushedObjectMultiBulkReply() {
    return (List<Object>) readProtocolWithCheckingBroken();
  }

  @SuppressWarnings("unchecked")
  public Object getUnflushedObject() {
    return readProtocolWithCheckingBroken();
  }

  public List<Object> getObjectMultiBulkReply() {
    flush();
    return (List<Object>) readProtocolWithCheckingBroken();
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
      return Protocol.read(inputStream, clientSideCache);
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

  /**
   * Check if the client name libname, libver, characters are legal
   * @param info the name
   * @return Returns true if legal, false throws exception
   * @throws JedisException if characters illegal
   */
  private static boolean validateClientInfo(String info) {
    for (int i = 0; i < info.length(); i++) {
      char c = info.charAt(i);
      if (c < '!' || c > '~') {
        throw new JedisValidationException("client info cannot contain spaces, "
            + "newlines or special characters.");
      }
    }
    return true;
  }

  private void initializeFromClientConfig(final JedisClientConfig config) {
    try {
      connect();

      protocol = config.getRedisProtocol();

      final Supplier<RedisCredentials> credentialsProvider = config.getCredentialsProvider();
      if (credentialsProvider instanceof RedisCredentialsProvider) {
        final RedisCredentialsProvider redisCredentialsProvider = (RedisCredentialsProvider) credentialsProvider;
        try {
          redisCredentialsProvider.prepare();
          helloOrAuth(protocol, redisCredentialsProvider.get());
        } finally {
          redisCredentialsProvider.cleanUp();
        }
      } else {
        helloOrAuth(protocol, credentialsProvider != null ? credentialsProvider.get()
            : new DefaultRedisCredentials(config.getUser(), config.getPassword()));
      }

      List<CommandArguments> fireAndForgetMsg = new ArrayList<>();

      String clientName = config.getClientName();
      if (clientName != null && validateClientInfo(clientName)) {
        fireAndForgetMsg.add(new CommandArguments(Command.CLIENT).add(Keyword.SETNAME).add(clientName));
      }

      ClientSetInfoConfig setInfoConfig = config.getClientSetInfoConfig();
      if (setInfoConfig == null) setInfoConfig = ClientSetInfoConfig.DEFAULT;

      if (!setInfoConfig.isDisabled()) {
        String libName = JedisMetaInfo.getArtifactId();
        if (libName != null && validateClientInfo(libName)) {
          String libNameSuffix = setInfoConfig.getLibNameSuffix();
          if (libNameSuffix != null) { // validation is moved into ClientSetInfoConfig constructor
            libName = libName + '(' + libNameSuffix + ')';
          }
          fireAndForgetMsg.add(new CommandArguments(Command.CLIENT).add(Keyword.SETINFO)
              .add(ClientAttributeOption.LIB_NAME.getRaw()).add(libName));
        }

        String libVersion = JedisMetaInfo.getVersion();
        if (libVersion != null && validateClientInfo(libVersion)) {
          fireAndForgetMsg.add(new CommandArguments(Command.CLIENT).add(Keyword.SETINFO)
              .add(ClientAttributeOption.LIB_VER.getRaw()).add(libVersion));
        }
      }

      for (CommandArguments arg : fireAndForgetMsg) {
        sendCommand(arg);
      }
      getMany(fireAndForgetMsg.size());

      int dbIndex = config.getDatabase();
      if (dbIndex > 0) {
        select(dbIndex);
      }

    } catch (JedisException je) {
      try {
        disconnect();
      } catch (Exception e) {
        // the first exception 'je' will be thrown
      }
      throw je;
    }
  }

  private void helloOrAuth(final RedisProtocol protocol, final RedisCredentials credentials) {

    if (credentials == null || credentials.getPassword() == null) {
      if (protocol != null) {
        sendCommand(Command.HELLO, encode(protocol.version()));
        getOne();
      }
      return;
    }

    // Source: https://stackoverflow.com/a/9670279/4021802
    ByteBuffer passBuf = Protocol.CHARSET.encode(CharBuffer.wrap(credentials.getPassword()));
    byte[] rawPass = Arrays.copyOfRange(passBuf.array(), passBuf.position(), passBuf.limit());
    Arrays.fill(passBuf.array(), (byte) 0); // clear sensitive data

    try {
      /// actual HELLO or AUTH -->
      if (protocol != null) {
        if (credentials.getUser() != null) {
          sendCommand(Command.HELLO, encode(protocol.version()),
              Keyword.AUTH.getRaw(), encode(credentials.getUser()), rawPass);
          getOne(); // Map
        } else {
          sendCommand(Command.AUTH, rawPass);
          getStatusCodeReply(); // OK
          sendCommand(Command.HELLO, encode(protocol.version()));
          getOne(); // Map
        }
      } else { // protocol == null
        if (credentials.getUser() != null) {
          sendCommand(Command.AUTH, encode(credentials.getUser()), rawPass);
        } else {
          sendCommand(Command.AUTH, rawPass);
        }
        getStatusCodeReply(); // OK
      }
      /// <-- actual HELLO or AUTH
    } finally {

      Arrays.fill(rawPass, (byte) 0); // clear sensitive data
    }

    // clearing 'char[] credentials.getPassword()' should be
    // handled in RedisCredentialsProvider.cleanUp()
  }

  public String select(final int index) {
    sendCommand(Command.SELECT, Protocol.toByteArray(index));
    return getStatusCodeReply();
  }

  public boolean ping() {
    sendCommand(Command.PING);
    String status = getStatusCodeReply();
    if (!"PONG".equals(status)) {
      throw new JedisException(status);
    }
    return true;
  }

  private void initializeClientSideCache(ClientSideCache csCache) {
    this.clientSideCache = csCache;
    if (clientSideCache != null) {
      if (protocol != RedisProtocol.RESP3) {
        throw new JedisException("Client side caching is only supported with RESP3.");
      }

      sendCommand(Protocol.Command.CLIENT, "TRACKING", "ON");
      String reply = getStatusCodeReply();
      if (!"OK".equals(reply)) {
        throw new JedisException("Could not enable client tracking. Reply: " + reply);
      }
    }
  }
}
