package redis.clients.jedis;

import static redis.clients.jedis.MaintenanceNotificationsConfig.Mode.ENABLED;
import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.NumberUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class Connection implements Closeable {
  public static Logger logger = LoggerFactory.getLogger(Connection.class);

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
  /**
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for non-blocking commands.
   * A value of {@code 0} indicates an infinite timeout.
   */
  private int soTimeout = 0;

  /**
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for blocking commands.
   * A value of {@code 0} indicates an infinite timeout.
   */
  private int infiniteSoTimeout = 0;

  /**
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for non-blocking commands
   * while the connection is in a relaxed (maintenance) state.
   *
   * <p>If disabled (see {@link TimeoutOptions#DISABLED_TIMEOUT}), {@link #soTimeout}
   * is used instead.</p>
   */
  private int relaxedTimeout =  NumberUtils.safeToInt(Duration.ofSeconds(10).toMillis());

  /**
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for blocking commands
   * while the connection is in a relaxed (maintenance) state.
   *
   * <p>If disabled (see {@link TimeoutOptions#DISABLED_TIMEOUT}), {@link #infiniteSoTimeout}
   * is used instead.</p>
   */
  private int relaxedBlockingTimeout =
          NumberUtils.safeToInt(TimeoutOptions.DISABLED_TIMEOUT.toMillis());

  private boolean broken = false;
  private boolean strValActive;
  private String strVal;
  protected String server;
  protected String version;
  private AtomicReference<RedisCredentials> currentCredentials = new AtomicReference<>(null);
  private AuthXManager authXManager;
  private boolean isBlocking = false;
  private boolean isRelaxed = false;
  private JedisClientConfig clientConfig;
  private final PushConsumerChainImpl pushConsumers = PushConsumerChainImpl.of();
  private boolean rebindRequested = false;

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
    this.authXManager = null;

    initPushConsumers(null);
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

  /**
   * Initializes the default {@link PushConsumer}s used to process incoming push messages.
   *
   * <p>The consumer chain is configured as follows:</p>
   *
   * <ul>
   *   <li><b>Pub/Sub consumer</b> – Handles Pub/Sub messages and propagates them back to the caller.
   *       All other message types are considered processed and are not propagated further.</li>
   *   <li><b>Maintenance event consumer</b> (optional) – Handles server-side maintenance notifications
   *       when enabled via {@link JedisClientConfig#maintNotificationsConfig()}.</li>
   * </ul>
   *
   * <p>The maintenance consumer is responsible for:</p>
   * <ul>
   *   <li>Per-connection handling (e.g. timeout relaxation, rebind flags)</li>
   *   <li>Notifying pool-level components (e.g. factory rebind, pool clear) via the owning connection</li>
   * </ul>
   *
   * @param config the client configuration; if {@code null}, only default consumers are registered
   */
  protected void initPushConsumers(JedisClientConfig config) {

    // Register default Pub/Sub consumer.
    // Propagates Pub/Sub messages to the caller; all other messages are consumed.
    addPushConsumer(PushConsumerChainImpl.PUBSUB_CONSUMER);

    if (config != null && config.maintNotificationsConfig().isEnabled()) {

      // Register maintenance event consumer.
      // Handles server maintenance notifications and triggers connection/pool-level actions.
      addPushConsumer(new MaintenanceEventConsumer(true)); // proactiveRebindEnabled = true
    }
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

  /**
   * Returns the socket read timeout (SO_TIMEOUT) in milliseconds used for non-blocking commands.
   *
   * @return the configured timeout in milliseconds for non-blocking operations
   * @see #getRelaxedSoTimeout()
   */
  public int getSoTimeout() {
    return soTimeout;
  }

  /**
   * Returns the socket read timeout (SO_TIMEOUT) in milliseconds used for blocking commands.
   *
   * <p>This timeout is applied to blocking Redis operations (e.g. BLPOP, BRPOP) where the
   * connection is expected to wait for a potentially long or indefinite period.</p>
   *
   * @return the configured timeout in milliseconds for blocking operations
   * @see #getSoTimeout()
   */
  public int getBlockingSoTimeout() {
    return infiniteSoTimeout;
  }



  /**
   * Returns the socket read timeout (SO_TIMEOUT) in milliseconds used for non-blocking commands
   * while the connection is in a relaxed (maintenance) state.
   *
   * <p>This timeout is applied when relaxed timeout mode is active (see {@link #isRelaxedTimeoutActive()}).</p>
   *
   * @return the relaxed timeout in milliseconds for non-blocking operations during maintenance
   * @see #getSoTimeout()
   * @see #isRelaxedTimeoutActive()
   */
  public int getRelaxedSoTimeout() {
    return relaxedTimeout;
  }

  /**
   * Returns the socket read timeout (SO_TIMEOUT) in milliseconds used for blocking commands
   * while the connection is in a relaxed (maintenance) state.
   *
   * <p>This timeout is applied when relaxed timeout mode is active
   * (see {@link #isRelaxedTimeoutActive()}).</p>
   *
   * <p>It is specifically used for blocking Redis commands (e.g. BLPOP, BRPOP) during
   * maintenance periods where the connection may require different timeout behavior.</p>
   *
   * @return the relaxed timeout in milliseconds for blocking operations during maintenance
   * @see #getRelaxedSoTimeout()
   * @see #isRelaxedTimeoutActive()
   */
  public int getRelaxedBlockingSoTimeout() {
    return relaxedBlockingTimeout;
  }

  /**
   * Sets the socket read timeout (SO_TIMEOUT) in milliseconds for non-blocking commands.
   *
   * <p>The configured timeout is applied to the underlying socket immediately if the connection
   * is already established. Otherwise, it will be applied when the socket is created.</p>
   *
   * <p>If the connection is currently in a <em>relaxed timeout</em> state (see {@link #isRelaxedTimeoutActive()}),
   * the effective timeout applied to the socket will be the relaxed timeout value instead of the provided one.
   * Once the relaxed state is disabled, the configured timeout will take effect again.</p>
   *
   * @param soTimeout the timeout value in milliseconds; a value of {@code 0} means infinite timeout
   * @throws JedisConnectionException if the underlying socket fails to apply the timeout
   * @see #isRelaxedTimeoutActive()
   */
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
    if (this.socket != null) {
      try {
        this.socket.setSoTimeout(getActiveSoTimeout());
      } catch (SocketException ex) {
        setBroken();
        throw new JedisConnectionException(ex);
      }
    }
  }

  /**
   * Sets the socket read timeout (SO_TIMEOUT) to infinite for blocking commands.
   *
   * <p>This method ensures the underlying socket is connected before applying the timeout.</p>
   *
   * <p>The effective timeout applied depends on the current connection state:</p>
   * <ul>
   *   <li>If relaxed timeout mode is active, the relaxed blocking timeout is used.</li>
   *   <li>Otherwise, the configured infinite blocking timeout is applied.</li>
   * </ul>
   *
   * <p>This is typically used for blocking Redis commands (e.g. BLPOP, BRPOP) where
   * the connection is expected to wait indefinitely for server responses.</p>
   *
   * @throws JedisConnectionException if the socket cannot be configured or connection fails
   * @see #getActiveBlockingSoTimeout()
   * @see #isRelaxedTimeoutActive()
   */
  public void setTimeoutInfinite() {
    try {
      if (!isConnected()) {
        connect();
      }
      socket.setSoTimeout(getActiveBlockingSoTimeout());
    } catch (SocketException ex) {
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  /**
   * Restores the socket read timeout (SO_TIMEOUT) to the currently active non-blocking timeout.
   *
   * <p>This method is typically called after a blocking operation completes to restore
   * the connection's normal timeout behavior.</p>
   *
   * <p>The restored timeout depends on the current connection state:</p>
   * <ul>
   *   <li>{@link #getRelaxedSoTimeout()} if relaxed timeout mode is active</li>
   *   <li>{@link #getSoTimeout()} otherwise</li>
   * </ul>
   *
   * @throws JedisConnectionException if the socket cannot be reconfigured
   * @see #getActiveSoTimeout()
   */
  public void rollbackTimeout() {
    try {
      socket.setSoTimeout(getActiveSoTimeout());
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
        isBlocking = true;
        setTimeoutInfinite();
        return commandObject.getBuilder().build(getOne());
      } finally {
        isBlocking = false;
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
      if (isBroken() || isRebindRequested()) {
        pool.returnBrokenResource(this);
      } else {
        pool.returnResource(this);
      }
    } else {
      disconnect();
    }
  }

  private boolean isRebindRequested() {
    return rebindRequested;
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
  protected Object protocolRead(RedisInputStream is, PushConsumerChain consumer) {
    return Protocol.read(is, consumer);
  }

  @Experimental
  protected void protocolReadPushes(RedisInputStream is, PushConsumerChain consumer) {
  }

  protected Object readProtocolWithCheckingBroken() {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection.");
    }

    try {
      return protocolRead(inputStream, pushConsumers);
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
        protocolReadPushes(inputStream, pushConsumers);
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

      // Get timeout options from maintenance notifications config
      MaintenanceNotificationsConfig maintConfig = config.maintNotificationsConfig();
      if (maintConfig.isEnabled()) {
        TimeoutOptions timeoutOptions = maintConfig.getTimeoutOptions();
        this.relaxedTimeout = NumberUtils.safeToInt(timeoutOptions.getRelaxedTimeout().toMillis());
        this.relaxedBlockingTimeout = NumberUtils.safeToInt(timeoutOptions.getRelaxedBlockingTimeout().toMillis());
      }

      initPushConsumers(config);

      connect();

      protocol = config.getRedisProtocol();

      Supplier<RedisCredentials> credentialsProvider = config.getCredentialsProvider();

      authXManager = config.getAuthXManager();
      if (authXManager != null) {
        credentialsProvider = authXManager;
      }

      if (credentialsProvider instanceof RedisCredentialsProvider) {
        final RedisCredentialsProvider redisCredentialsProvider = (RedisCredentialsProvider) credentialsProvider;
        try {
          redisCredentialsProvider.prepare();
          helloAndAuth(protocol, redisCredentialsProvider.get());
        } finally {
          redisCredentialsProvider.cleanUp();
        }
      } else {
        helloAndAuth(protocol, credentialsProvider != null ? credentialsProvider.get()
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

      MaintenanceNotificationsConfig maintNotificationsConfig = config.maintNotificationsConfig();
      if (maintNotificationsConfig.isEnabled()) {
        // TODO: Store movingTargetEndpointType if needed
        sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON");
        try {
          getStatusCodeReply();
        } catch (JedisDataException e) {
          // command not supported, fail connection
          if (maintNotificationsConfig.getMode() == ENABLED) {
            throw e;
          } else {
            // auto mode - ignore error
            // failback to connection without maintenance notifications support
          }
        }
      }

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

  private void helloAndAuth(final RedisProtocol protocol, final RedisCredentials credentials) {
    Map<String, Object> helloResult = null;
    if (protocol != null && credentials != null && credentials.getUser() != null) {
      byte[] rawPass = encodeToBytes(credentials.getPassword());
      try {
        helloResult = hello(encode(protocol.version()), Keyword.AUTH.getRaw(),
          encode(credentials.getUser()), rawPass);
      } finally {
        Arrays.fill(rawPass, (byte) 0); // clear sensitive data
      }
    } else {
      authenticate(credentials);
      helloResult = protocol == null ? null : hello(encode(protocol.version()));
    }
    if (helloResult != null) {
      server = (String) helloResult.get("server");
      version = (String) helloResult.get("version");
    }

    // clearing 'char[] credentials.getPassword()' should be
    // handled in RedisCredentialsProvider.cleanUp()
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

  /**
   * Returns an unmodifiable view of the registered push consumers.
   *
   * @return
   */
  List<PushConsumer> getPushConsumers() {
    return pushConsumers.getConsumers();
  }

  @Internal
  protected void addPushConsumer(PushConsumer consumer) {
    this.pushConsumers.add(consumer);
  }

  @Experimental
  public boolean isRelaxedTimeoutActive() {
    return isRelaxed;
  }

  int getActiveSoTimeout() {
    if (isRelaxed) {
      return TimeoutOptions.isRelaxedTimeoutEnabled(relaxedTimeout) ? relaxedTimeout : soTimeout;
    }

    return soTimeout;
  }


  int getActiveBlockingSoTimeout() {
    if (isRelaxed) {
      return TimeoutOptions.isRelaxedTimeoutEnabled(relaxedBlockingTimeout) ? relaxedBlockingTimeout : infiniteSoTimeout;
    }

    return infiniteSoTimeout;
  }

  void activateRelaxedTimeout() {
    isRelaxed = true;
    try {
      if (isBlocking){
        socket.setSoTimeout(getActiveBlockingSoTimeout());
      }  else {
        socket.setSoTimeout(getActiveSoTimeout());
      }
    } catch (SocketException ex) {
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  void rollbackRelaxedTimeout(){
    isRelaxed = false;
    try {
      if (isBlocking){
        socket.setSoTimeout(getActiveBlockingSoTimeout());
      }  else {
        socket.setSoTimeout(getActiveSoTimeout());
      }
    } catch (SocketException ex) {
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  /**
   * Push consumer that handles server maintenance events.
   * <p>
   * Handles per-connection concerns (timeout relaxation, rebind flag) inline.
   * Notifies the owning {@link ConnectionPool} via {@code memberOf} for pool-level concerns.
   * </p>
   */
  class MaintenanceEventConsumer implements PushConsumer {
    private final boolean proactiveRebindEnabled;

    public MaintenanceEventConsumer(boolean proactiveRebindEnabled) {
      this.proactiveRebindEnabled = proactiveRebindEnabled;
    }


    @Override
    public PushConsumerContext handle(PushConsumerContext context) {
      PushMessage message = context.getMessage();

      switch (message.getType()) {
      case "MOVING":
        onMoving(message);
        context.drop();
        break;
      case "MIGRATING":
        activateRelaxedTimeout();
        context.drop();
        break;
      case "MIGRATED":
        rollbackRelaxedTimeout();
        context.drop();
        break;
      case "FAILING_OVER":
        activateRelaxedTimeout();
        context.drop();
        break;
      case "FAILED_OVER":
        rollbackRelaxedTimeout();
        context.drop();
        break;
      }

      return context;
    }

    private void onMoving(PushMessage message) {
      HostAndPort rebindTarget = getRebindTarget(message);

      // per-connection: mark rebind requested and relax timeouts
      if (proactiveRebindEnabled) {
        rebindRequested = true;
      }
      activateRelaxedTimeout();

      // notify owning pool to rebind factory and clear idle connections
      if (memberOf != null) {
        memberOf.onMoving(rebindTarget);
      }
    }

    private HostAndPort getRebindTarget(PushMessage message) {
      // Extract domain/ip and port from the message
      // MOVING push message format: ["MOVING", slot, "host:port"]
      List<Object> content = message.getContent();

      if (content.size() < 3) {
        logger.warn("MOVING push message is malformed: {}", message);
        return null;
      }

      Object addressObject = content.get(2); // Get the 3rd element (index 2)
      if (!(addressObject instanceof byte[])) {
        logger.warn("Invalid re-bind message format, expected 3rd element to be a byte[], got {}", addressObject);
        return null;
      }


      try {
        String addressAndPort = SafeEncoder.encode((byte[]) addressObject);
        return HostAndPort.from(addressAndPort);
      } catch (Exception e) {
        logger.warn("Error parsing re-bind target from message: {}", message, e);
        return null;
      }
    }

  }
}
