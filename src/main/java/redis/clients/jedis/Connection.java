package redis.clients.jedis;

import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

public class Connection implements Closeable {

  public static class Builder {
    private JedisSocketFactory socketFactory;
    private JedisClientConfig clientConfig;

    public Builder socketFactory(JedisSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
      return this;
    }

    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public JedisSocketFactory getSocketFactory() {
      return socketFactory;
    }

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    public Connection build() {
      Connection conn = new Connection(this);
      conn.initializeFromClientConfig();
      return conn;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private ConnectionPool memberOf;
  protected RedisProtocol protocol;
  private final JedisSocketFactory socketFactory;
  private Socket socket;
  private RedisOutputStream outputStream;
  private RedisInputStream inputStream;
  private int soTimeout = 0;
  private int infiniteSoTimeout = 0;
  private boolean broken = false;
  private boolean strValActive;
  private String strVal;
  protected String server;
  protected String version;
  private AtomicReference<RedisCredentials> currentCredentials = new AtomicReference<>(null);
  private AuthXManager authXManager;
  private JedisClientConfig clientConfig;

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
    this.clientConfig = clientConfig;
    initializeFromClientConfig(clientConfig);
  }

  protected Connection(Builder builder) {
    this.socketFactory = builder.getSocketFactory();
    this.clientConfig = builder.getClientConfig();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + socketFactory + "}";
  }

  @Experimental
  public String toIdentityString() {
    if (strValActive == broken && strVal != null) {
      return strVal;
    }

    String className = getClass().getSimpleName();
    int id = hashCode();

    if (socket == null) {
      return String.format("%s{id: 0x%X}", className, id);
    }

    SocketAddress remoteAddr = socket.getRemoteSocketAddress();
    SocketAddress localAddr = socket.getLocalSocketAddress();
    if (remoteAddr != null) {
      strVal = String.format("%s{id: 0x%X, L:%s %c R:%s}", className, id, localAddr,
        (broken ? '!' : '-'), remoteAddr);
    } else if (localAddr != null) {
      strVal = String.format("%s{id: 0x%X, L:%s}", className, id, localAddr);
    } else {
      strVal = String.format("%s{id: 0x%X}", className, id);
    }

    strValActive = broken;
    return strVal;
  }

  public final RedisProtocol getRedisProtocol() {
    return protocol;
  }

  public final void setHandlingPool(final ConnectionPool pool) {
    this.memberOf = pool;
  }

  /**
   * Returns the host and port of the Redis server this connection is connected to.
   *
   * @return the host and port, or null if not available
   */
  public final HostAndPort getHostAndPort() {
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
        setBroken();
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
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  public void rollbackTimeout() {
    try {
      socket.setSoTimeout(this.soTimeout);
    } catch (SocketException ex) {
      setBroken();
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
      setBroken();
      throw ex;
    }
  }

  public void connect() throws JedisConnectionException {
    if (!isConnected()) {
      try {
        socket = socketFactory.createSocket();
        soTimeout = socket.getSoTimeout(); // ?

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

  public void forceDisconnect() throws IOException {
    // setBroken() must be called first here,
    // otherwise a concurrent close attempt would call 'returnResource' (instead of
    // 'returnBrokenResource'),
    // assuming it's an open/healthy connection whereas this individual socket is already closed.
    setBroken();
    IOUtils.closeQuietly(socket);
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
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  @Experimental
  protected Object protocolRead(RedisInputStream is) {
    return Protocol.read(is);
  }

  @Experimental
  protected void protocolReadPushes(RedisInputStream is) {
  }

  protected Object readProtocolWithCheckingBroken() {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection.");
    }

    try {
      return protocolRead(inputStream);
    } catch (JedisConnectionException exc) {
      broken = true;
      throw exc;
    }
  }

  protected void readPushesWithCheckingBroken() {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection.");
    }

    try {
      if (inputStream.available() > 0) {
        protocolReadPushes(inputStream);
      }
    } catch (IOException e) {
      broken = true;
      throw new JedisConnectionException("Failed to check buffer on connection.", e);
    } catch (JedisConnectionException exc) {
      setBroken();
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
        throw new JedisValidationException(
            "client info cannot contain spaces, " + "newlines or special characters.");
      }
    }
    return true;
  }

  public void initializeFromClientConfig() {
    this.initializeFromClientConfig(clientConfig);
  }

  protected void initializeFromClientConfig(final JedisClientConfig config) {
    try {
      this.soTimeout = config.getSocketTimeoutMillis();
      this.infiniteSoTimeout = config.getBlockingSocketTimeoutMillis();

      connect();

      RedisProtocol requestedProtocol = config.getRedisProtocol();

      Supplier<RedisCredentials> credentialsProvider = config.getCredentialsProvider();

      authXManager = config.getAuthXManager();
      if (authXManager != null) {
        credentialsProvider = authXManager;
      }

      if (credentialsProvider instanceof RedisCredentialsProvider) {
        final RedisCredentialsProvider redisCredentialsProvider = (RedisCredentialsProvider) credentialsProvider;
        try {
          redisCredentialsProvider.prepare();
          protocol = helloAndAuth(requestedProtocol, redisCredentialsProvider.get());
        } finally {
          redisCredentialsProvider.cleanUp();
        }
      } else {
        protocol = helloAndAuth(requestedProtocol, credentialsProvider != null ? credentialsProvider.get()
            : new DefaultRedisCredentials(config.getUser(), config.getPassword()));
      }

      List<CommandArguments> fireAndForgetMsg = new ArrayList<>();

      String clientName = config.getClientName();
      if (clientName != null && validateClientInfo(clientName)) {
        fireAndForgetMsg
            .add(new CommandArguments(Command.CLIENT).add(Keyword.SETNAME).add(clientName));
      }

      ClientSetInfoConfig setInfoConfig = config.getClientSetInfoConfig();
      if (setInfoConfig == null) {
        setInfoConfig = ClientSetInfoConfig.DEFAULT;
      }

      if (!setInfoConfig.isDisabled()) {
        fireAndForgetMsg.add(
                new CommandArguments(Command.CLIENT).add(Keyword.SETINFO).add(ClientAttributeOption.LIB_NAME.getRaw())
                        .add(setInfoConfig.getDriverInfo().getFormattedName()));

        String libVersion = JedisMetaInfo.getVersion();
        if (libVersion != null && validateClientInfo(libVersion)) {
          fireAndForgetMsg.add(
                  new CommandArguments(Command.CLIENT).add(Keyword.SETINFO).add(ClientAttributeOption.LIB_VER.getRaw())
                          .add(libVersion));
        }
      }

      // set READONLY flag to ALL connections (including master nodes) when enable read from replica
      if (config.isReadOnlyForRedisClusterReplicas()) {
        fireAndForgetMsg.add(new CommandArguments(Command.READONLY));
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

  /**
   * Performs HELLO and/or AUTH handshake and returns the resolved protocol version.
   * <p>
   * For {@link RedisProtocol#RESP3_PREFERRED}, attempts HELLO 3 and falls back to RESP2 on failure.
   *
   * @param protocol the requested protocol (may be {@code null} or {@code RESP3_PREFERRED})
   * @param credentials the credentials for authentication
   * @return the actual negotiated protocol version
   */
  private RedisProtocol helloAndAuth(final RedisProtocol protocol, final RedisCredentials credentials) {
    final String helloVersion = protocol == RedisProtocol.RESP3_PREFERRED
        ? RedisProtocol.RESP3.version() : protocol != null ? protocol.version() : null;

    Map<String, Object> helloResult = null;
    if (protocol != null && credentials != null && credentials.getUser() != null) {
      byte[] rawPass = encodeToBytes(credentials.getPassword());
      try {
        helloResult = hello(encode(helloVersion), Keyword.AUTH.getRaw(),
          encode(credentials.getUser()), rawPass);

        // NOTE(imalinovskyi): We assume that when username is provided, client is connecting Redis 6.0+,
        // so we don't need to catch JedisUnknownCommandException here.
      } catch (JedisAccessControlException e) {
        // Redis 6.0.x (before 6.2.2) has a bug where HELLO with AUTH fails if the default user
        // requires authentication — the server demands AUTH before allowing HELLO.
        // See: https://github.com/redis/redis/issues/8558
        // See: https://github.com/redis/lettuce/issues/2592
        if (protocol == RedisProtocol.RESP3_PREFERRED) {
          // Fall back: AUTH first, then try HELLO without auth
          authenticate(credentials);
          try {
            helloResult = hello(encode(helloVersion));
          } catch (JedisDataException e2) {
            // HELLO still fails (e.g., RESP3 not supported), fall back to RESP2
            return RedisProtocol.RESP2;
          }
        } else {
          throw e;
        }
      } finally {
        Arrays.fill(rawPass, (byte) 0); // clear sensitive data
      }
    } else {
      authenticate(credentials);
      if (protocol != null) {
        try {
          helloResult = hello(encode(helloVersion));
        } catch (JedisUnknownCommandException e) {
          if (protocol == RedisProtocol.RESP3_PREFERRED) {
            // HELLO not supported, fall back to RESP2
            return RedisProtocol.RESP2;
          }
          throw new JedisProtocolNotSupportedException("HELLO is not supported by the server. "
              + "Please check the server version or disable protocol version specification with serverDefaultProtocol().", e);
        } catch (JedisDataException e) {
          if (protocol == RedisProtocol.RESP3_PREFERRED) {
            // RESP3 not supported, fall back to RESP2
            return RedisProtocol.RESP2;
          }
          throw e;
        }
      }
    }
    if (helloResult != null) {
      server = (String) helloResult.get("server");
      version = (String) helloResult.get("version");
      // HELLO succeeded — resolve RESP3_PREFERRED to actual RESP3
      if (protocol == RedisProtocol.RESP3_PREFERRED) {
        return RedisProtocol.RESP3;
      }
    }

    // clearing 'char[] credentials.getPassword()' should be
    // handled in RedisCredentialsProvider.cleanUp()
    return protocol;
  }

  public void setCredentials(RedisCredentials credentials) {
    currentCredentials.set(credentials);
  }

  private String authenticate(RedisCredentials credentials) {
    if (credentials == null || credentials.getPassword() == null) {
      return null;
    }
    byte[] rawPass = encodeToBytes(credentials.getPassword());
    try {
      if (credentials.getUser() == null) {
        sendCommand(Command.AUTH, rawPass);
      } else {
        sendCommand(Command.AUTH, encode(credentials.getUser()), rawPass);
      }
    } finally {
      Arrays.fill(rawPass, (byte) 0); // clear sensitive data
    }
    return getStatusCodeReply();
  }

  public String reAuthenticate() {
    return authenticate(currentCredentials.getAndSet(null));
  }

  protected Map<String, Object> hello(byte[]... args) {
    sendCommand(Command.HELLO, args);
    return BuilderFactory.ENCODED_OBJECT_MAP.build(getOne());
  }

  protected byte[] encodeToBytes(char[] chars) {
    // Source: https://stackoverflow.com/a/9670279/4021802
    ByteBuffer passBuf = Protocol.CHARSET.encode(CharBuffer.wrap(chars));
    byte[] rawPass = Arrays.copyOfRange(passBuf.array(), passBuf.position(), passBuf.limit());
    Arrays.fill(passBuf.array(), (byte) 0); // clear sensitive data
    return rawPass;
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

  protected boolean isTokenBasedAuthenticationEnabled() {
    return authXManager != null;
  }

  protected AuthXManager getAuthXManager() {
    return authXManager;
  }
}
