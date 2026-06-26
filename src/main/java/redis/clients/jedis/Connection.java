package redis.clients.jedis;

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
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.concurrent.CopyOnWriteArrayList;
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
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

public class Connection implements Closeable {
  public static Logger logger = LoggerFactory.getLogger(Connection.class);

  public static class Builder {
    private JedisSocketFactory socketFactory;
    private JedisClientConfig clientConfig;
    private MaintenanceNotificationsConfig maintenanceConfig;
    private final List<MaintenanceEventListener> maintenanceEventListeners = new ArrayList<>();
    private SoTimeoutSupplier soTimeoutSupplier;

    public Builder socketFactory(JedisSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
      return this;
    }

    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    /**
     * Configuration that drives the {@code CLIENT MAINT_NOTIFICATIONS} handshake (mode, endpoint
     * type). {@code null} (or {@code DISABLED}) skips the handshake.
     */
    public Builder maintenanceConfig(MaintenanceNotificationsConfig maintenanceConfig) {
      this.maintenanceConfig = maintenanceConfig;
      return this;
    }

    /** Registers a listener notified synchronously of this connection's maintenance events. */
    Builder addMaintenanceEventListener(MaintenanceEventListener listener) {
      this.maintenanceEventListeners.add(listener);
      return this;
    }

    /**
     * Installs a per-operation {@code SO_TIMEOUT} override (see {@link SoTimeoutSupplier}). Absent a
     * supplier the connection uses its configured timeouts unchanged.
     */
    Builder soTimeoutSupplier(SoTimeoutSupplier soTimeoutSupplier) {
      this.soTimeoutSupplier = soTimeoutSupplier;
      return this;
    }

    public JedisSocketFactory getSocketFactory() {
      return socketFactory;
    }

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    MaintenanceNotificationsConfig getMaintenanceConfig() {
      return maintenanceConfig;
    }

    List<MaintenanceEventListener> getMaintenanceEventListeners() {
      return maintenanceEventListeners;
    }

    SoTimeoutSupplier getSoTimeoutSupplier() {
      return soTimeoutSupplier;
    }

    public Connection build() {
      Connection conn = createConnection();
      conn.initializeFromClientConfig();
      return conn;
    }

    /**
     * Construct a fresh, uninitialized {@link Connection} for this builder.
     * <p>
     * Template-method hook for subclasses to return their concrete {@code Connection} subtype
     * (see {@link redis.clients.jedis.csc.CacheConnection.Builder} for an example). The
     * returned instance has its socket factory and client config wired up, but the network
     * handshake has not run yet.
     * <p>
     * Two callers drive initialization differently:
     * <ul>
     *   <li>{@link #build()} — the public entry point — invokes
     *       {@link Connection#initializeFromClientConfig()} on the returned instance before
     *       handing it back, so external callers receive a ready-to-use connection.</li>
     *   <li>{@link #buildUninitialized()} — the package-private accessor used by pool
     *       factories — returns the uninitialized instance so the factory can interpose
     *       between construction and initialization (e.g. to track an in-flight handshake
     *       so a forced disconnect can interrupt it).</li>
     * </ul>
     * <p>
     * Implementations must be free of side effects: do not open sockets, send commands, or
     * invoke {@link Connection#initializeFromClientConfig()} — running initialization here
     * would break the {@code buildUninitialized()} contract that pool factories rely on.
     *
     * @return a freshly constructed, uninitialized {@code Connection}
     */
    protected Connection createConnection() {
      return new Connection(this);
    }

    /**
     * Package-private entry point for pool factories that need to interpose between construction
     * and initialization (e.g. to track in-flight connections so a forced disconnect can
     * interrupt the init handshake). Callers MUST invoke
     * {@link Connection#initializeFromClientConfig()} exactly once before handing the connection
     * out.
     */
    final Connection buildUninitialized() {
      return createConnection();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private ConnectionPool memberOf;
  /**
   * The RESP protocol established for this connection.
   *
   * <p>Set after the handshake completes. Holds the protocol the connection is actually
   * speaking with the server.</p>
   *
   * <ul>
   *   <li>{@code null} – No protocol established yet (handshake has not run).</li>
   *   <li>{@code RESP2} – RESP2 established (either explicitly requested, fallback from
   *       auto-negotiation, or assumed default when no protocol was requested and
   *       auto-negotiation was disabled).</li>
   *   <li>{@code RESP3} – RESP3 established.</li>
   * </ul>
   */
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
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for non-blocking commands while the
   * connection is in a relaxed (maintenance) state. {@link JedisClientConfig#UNSET_TIMEOUT_MS}
   * means {@link #soTimeout} is inherited instead.
   */
  private int relaxedTimeout = MaintenanceNotificationsConfig.DEFAULT_RELAXED_SOCKET_TIMEOUT_MS;

  /**
   * Socket read timeout (SO_TIMEOUT) in milliseconds used for blocking commands while the
   * connection is in a relaxed (maintenance) state. {@link JedisClientConfig#UNSET_TIMEOUT_MS}
   * means {@link #infiniteSoTimeout} is inherited instead.
   */
  private int relaxedBlockingTimeout = MaintenanceNotificationsConfig.DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS;

  private boolean relaxedTimeoutConfigured = JedisClientConfig.isTimeoutSet(relaxedTimeout);

  private boolean relaxedBlockingTimeoutConfigured = JedisClientConfig
      .isTimeoutSet(relaxedBlockingTimeout);

  private boolean broken = false;
  private boolean strValActive;
  private String strVal;
  protected String server;
  protected String version;
  private AtomicReference<RedisCredentials> currentCredentials = new AtomicReference<>(null);
  private AuthXManager authXManager;
  private boolean isBlocking = false;

  /**
   * Maintenance relaxed-timeout overlay: {@code 0} = not relaxed, else the absolute
   * {@link #clockNanos} deadline until which relaxed timeouts apply. Reset to {@code 0} lazily on
   * the next read once expired.
   */
  private long relaxedUntilNanos = 0L;

  /** Monotonic clock for relaxed-timeout expiry; overridable for tests. */
  private LongSupplier clockNanos = System::nanoTime;

  /**
   * Maintenance configuration driving the {@code CLIENT MAINT_NOTIFICATIONS} handshake; {@code null}
   * when non-pooled or maintenance disabled.
   */
  private MaintenanceNotificationsConfig maintenanceConfig;

  /** Listeners notified synchronously of this connection's maintenance events (pool-injected). */
  private final List<MaintenanceEventListener> maintenanceEventListeners = new CopyOnWriteArrayList<>();

  /** Per-operation SO_TIMEOUT override; {@code null} = use configured timeouts unchanged. */
  private SoTimeoutSupplier soTimeoutSupplier;

  /**
   * Cache of the SO_TIMEOUT value currently applied to {@link #socket}. Lets {@link #applyTimeout}
   * skip the {@code setSoTimeout} syscall when the desired value already matches. Initialized in
   * {@link #connect()} from the socket's actual value and updated by {@link #applyTimeout}.
   */
  private int lastAppliedTimeout;

  private JedisClientConfig clientConfig;
  private final ProtocolHandshake handshake = new ProtocolHandshake(this);
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
    this.maintenanceConfig = builder.getMaintenanceConfig();              // pool-injected (may be null)
    this.maintenanceEventListeners.addAll(builder.getMaintenanceEventListeners());
    this.soTimeoutSupplier = builder.getSoTimeoutSupplier();              // pool-injected (may be null)
  }

  /**
   * Initializes the default {@link PushConsumer}s used to process incoming push messages.
   *
   * <p>The consumer chain is configured as follows:</p>
   *
   * <ul>
   *   <li><b>Pub/Sub consumer</b> – Handles Pub/Sub messages and propagates them back to the caller.
   *       All other message types are considered processed and are not propagated further.</li>
   *   <li><b>Maintenance event consumer</b> (optional) – Dispatches server maintenance
   *       notifications to the registered {@link MaintenanceEventListener}s. Registered only when
   *       maintenance is configured on the builder; non-pooled connections never have it.</li>
   * </ul>
   *
   * @param config the client configuration; if {@code null}, only default consumers are registered
   */
    private void initPushConsumers(JedisClientConfig config) {
    /*
     * Default consumers to process push messages.
     * Marks all @{link PushMessages as processed, except for pub/sub.
     * Pub/sub messages are propagated to the client.
     */
    addPushConsumer(PushConsumerChainImpl.PUBSUB_CONSUMER);
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

  /**
   * Returns the RESP protocol established for this connection.
   *
   * @return {@code null} if no protocol has been established yet (handshake has not run);
   *         {@link RedisProtocol#RESP2} or {@link RedisProtocol#RESP3} once the handshake
   *         has completed.
   */
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
  int getRelaxedSoTimeout() {
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
  int getRelaxedBlockingSoTimeout() {
    return relaxedBlockingTimeout;
  }

  /**
   * Sets the socket read timeout (SO_TIMEOUT) in milliseconds for non-blocking commands.
   *
   * <p>The configured timeout is applied to the underlying socket immediately if the connection
   * is already established. Otherwise, it will be applied when the socket is created.</p>
   *
   * <p>If the connection is currently in a <em>relaxed timeout</em> state (see {@link #isRelaxedTimeoutActive()}),
   * the relaxed value remains in effect on the socket; the value configured here takes effect
   * once the relaxation window closes.</p>
   *
   * @param soTimeout the timeout value in milliseconds; a value of {@code 0} means infinite timeout
   * @throws JedisConnectionException if the underlying socket fails to apply the timeout
   * @see #isRelaxedTimeoutActive()
   */
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
    applyTimeout(effectiveSoTimeout());
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
   * @see #isRelaxedTimeoutActive()
   */
  public void setTimeoutInfinite() {
    if (!isConnected()) {
      connect();
    }
    isBlocking = true;
    applyTimeout(effectiveSoTimeout());
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
   * @see #isRelaxedTimeoutActive()
   */
  public void rollbackTimeout() {
    isBlocking = false;
    applyTimeout(effectiveSoTimeout());
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
    }
    try {
      isBlocking = true;
      return commandObject.getBuilder().build(getOne());
    } finally {
      isBlocking = false;
      // Restore the non-blocking socket timeout so observers (and the next, possibly non-blocking,
      // command) see a coherent value immediately. Cache-checked → no syscall when unchanged.
      applyTimeout(effectiveSoTimeout());
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
        lastAppliedTimeout = soTimeout;    // mirror the freshly-set baseline

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
    // Hot-path fast skip: when no maintenance state is in play and we're not in a blocking
    // section, the socket's SO_TIMEOUT invariantly equals the configured baseline (set in
    // {@link #connect()} and maintained by {@link #applyTimeout}). Avoid the apply machinery in
    // that overwhelming common case. Otherwise, align the socket to the current blocking /
    // relaxation state — cache-checked, so a syscall fires only on a transition.
    if (relaxedUntilNanos != 0 || isBlocking || soTimeoutSupplier != null) {
      applyTimeout(effectiveSoTimeout());
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

  /**
   * Initialize this connection using the {@link JedisClientConfig} captured at construction.
   * <p>
   * Internal lifecycle step: invoked once by {@link Connection.Builder#build()} for direct
   * callers, and once by {@link ConnectionFactory#initialize(Connection)} for pooled
   * connections. There is no public construction path that produces an uninitialized-but-
   * configured {@code Connection}, so this method has no out-of-package use case.
   */
  void initializeFromClientConfig() {
    this.initializeFromClientConfig(clientConfig);
  }

  protected void initializeFromClientConfig(final JedisClientConfig config) {
    try {
      this.soTimeout = config.getSocketTimeoutMillis();
      this.infiniteSoTimeout = config.getBlockingSocketTimeoutMillis();

      // Relaxed timeouts are an SCH knob: sourced from the maintenance config when present,
      // otherwise the fields keep their defaults (unused, since relaxation never starts).
      if (maintenanceConfig != null) {
        this.relaxedTimeout = maintenanceConfig.getRelaxedSocketTimeoutMillis();
        this.relaxedBlockingTimeout = maintenanceConfig.getRelaxedBlockingSocketTimeoutMillis();
      }
      this.relaxedTimeoutConfigured = JedisClientConfig.isTimeoutSet(relaxedTimeout);
      this.relaxedBlockingTimeoutConfigured = JedisClientConfig.isTimeoutSet(relaxedBlockingTimeout);

      initPushConsumers(config);

      connect();

      RedisProtocol requestedProtocol = config.getRedisProtocol();
      boolean autoNegotiateProtocol = config.isAutoNegotiateProtocol();

      Supplier<RedisCredentials> credentialsProvider = config.getCredentialsProvider();

      authXManager = config.getAuthXManager();
      if (authXManager != null) {
        credentialsProvider = authXManager;
      }

      if (credentialsProvider instanceof RedisCredentialsProvider) {
        final RedisCredentialsProvider redisCredentialsProvider = (RedisCredentialsProvider) credentialsProvider;
        try {
          redisCredentialsProvider.prepare();
          establishProtocol(requestedProtocol, autoNegotiateProtocol,
            redisCredentialsProvider.get());
        } finally {
          redisCredentialsProvider.cleanUp();
        }
      } else {
        establishProtocol(requestedProtocol, autoNegotiateProtocol,
          credentialsProvider != null ? credentialsProvider.get()
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

      enableMaintenanceNotifications();

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
   * Activates server-side maintenance notifications on this connection so the client can react to
   * cluster maintenance events ({@code MIGRATING}, {@code MIGRATED}, {@code FAILING_OVER},
   * {@code FAILED_OVER}, {@code MOVING}). Each parsed event is dispatched synchronously to the
   * registered {@link MaintenanceEventListener}s.
   *
   * <p>Gated by {@link #maintenanceConfig}, set by the builder:
   *
   * <ul>
   *   <li>{@code null} or {@code DISABLED} — maintenance off; no-op.</li>
   *   <li>negotiated protocol is not RESP3 — {@code ENABLED}: throws; {@code AUTO}: debug-log
   *       and return.</li>
   *   <li>server rejects {@code CLIENT MAINT_NOTIFICATIONS ON} — same strict/lax split.</li>
   * </ul>
   *
   * @throws JedisConnectionException in {@code ENABLED} mode when a prerequisite is not met or the
   *           server rejects the subscription
   */
  private void enableMaintenanceNotifications() {
    if (maintenanceConfig == null
        || maintenanceConfig.getMode() == MaintenanceNotificationsConfig.Mode.DISABLED) {
      return;
    }
    boolean strict = maintenanceConfig.getMode() == MaintenanceNotificationsConfig.Mode.ENABLED;

    // Maintenance push frames require RESP3.
    if (protocol != RedisProtocol.RESP3) {
      String reason = "RESP3 is required but the established protocol is "
          + (protocol == null ? "RESP2" : protocol);
      if (strict) {
        throw new JedisConnectionException("Maintenance notifications: " + reason);
      }
      logger.debug("Maintenance notifications disabled: {}.", reason);
      return;
    }

    // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a push
    // frame the server emits immediately on accepting the subscription cannot race ahead.
    MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(this, maintenanceEventListeners);
    addPushConsumer(consumer);
    sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type",
      resolveEndpointType(maintenanceConfig.getEndpointType()));
    try {
      getStatusCodeReply();
    } catch (JedisDataException e) {
      removePushConsumer(consumer);
      if (strict) {
        throw new JedisConnectionException(
            "Maintenance notifications: events not supported on server", e);
      }
      logger.debug(
        "Maintenance notifications disabled: server rejected CLIENT MAINT_NOTIFICATIONS ({}).",
        e.getMessage());
    }
  }

  private String resolveEndpointType(MaintenanceNotificationsConfig.EndpointType endpointType) {
    switch (endpointType) {
      case INTERNAL_IP:
        return "internal-ip";
      case INTERNAL_FQDN:
        return "internal-fqdn";
      case EXTERNAL_IP:
        return "external-ip";
      case EXTERNAL_FQDN:
        return "external-fqdn";
      default:
        throw new JedisException("Unknown endpoint type: " + endpointType);
    }
  }

  /**
   * Establish the RESP protocol version and authenticate if needed.
   *
   * Performs protocol negotiation using the {@code HELLO} command and optional authentication,
   * and resolves the effective RESP protocol used for the connection.
   *
   * <p>This method supports both explicit protocol selection and legacy compatibility mode.</p>
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If {@code requestedProtocol} is {@code null} and {@code autoNegotiateProtocol} is
   *       {@code true}, the client first attempts {@code HELLO 3} and falls back to RESP2 if
   *       RESP3 is not supported.</li>
   *
   *   <li>If {@code requestedProtocol} is {@code null} and {@code autoNegotiateProtocol} is
   *       {@code false}, no {@code HELLO} is sent. The connection assumes RESP2 as the default
   *       protocol and only {@code AUTH} is performed if credentials are provided. This is the
   *       legacy {@link Jedis} behaviour.</li>
   *
   *   <li>If {@code RESP2} is requested, a strict {@code HELLO 2} handshake is performed.</li>
   *
   *   <li>If {@code RESP3} is requested, a strict {@code HELLO 3} handshake is performed.</li>
   * </ul>
   *
   * <p>Side effects:</p>
   * <ul>
   *   <li>Performs authentication if credentials are provided.</li>
   *   <li>Populates internal server metadata (version, server info).</li>
   *   <li>Stores the resolved protocol version for the connection.</li>
   * </ul>
   *
   * @param requestedProtocol the requested RESP protocol, or {@code null} to defer to
   *          {@code autoNegotiateProtocol}
   * @param autoNegotiateProtocol whether to attempt {@code HELLO 3} with RESP2 fallback when no
   *          protocol is explicitly requested; ignored when {@code requestedProtocol} is
   *          non-{@code null}
   * @param credentials credentials used for authentication (may be {@code null})
   * @throws IllegalArgumentException if the requested protocol is not supported
   * @throws IllegalStateException if the server's HELLO response is missing the protocol field
   * @throws JedisProtocolNotSupportedException if protocol negotiation fails
   * @throws JedisDataException if the server returns an error during handshake
   */
  private void establishProtocol(final RedisProtocol requestedProtocol,
      final boolean autoNegotiateProtocol, final RedisCredentials credentials) {
    HelloResult helloResult = handshake.establish(requestedProtocol, autoNegotiateProtocol,
      credentials);
    version = helloResult.getVersion();
    server = helloResult.getServer();

    if (helloResult.getProtocol() == null) {
      throw new IllegalStateException("HELLO response is missing the protocol version field");
    }

    this.protocol = helloResult.getProtocol();
  }

  public void setCredentials(RedisCredentials credentials) {
    currentCredentials.set(credentials);
  }

  String authenticate(RedisCredentials credentials) {
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

  /**
   * Sends the {@code HELLO} command to negotiate the RESP protocol version and optionally perform authentication.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If {@code credentials} is {@code null}, only protocol negotiation is performed
   *       (no {@code AUTH} is sent).</li>
   *
   *   <li>If {@code credentials} is provided and contains a password, authentication is
   *
   *       performed via {@code HELLO ... AUTH <user> <password>}.</li>
   *
   *   <li>If {@code credentials#getUser()} is {@code null}, the {@code default} user is used.</li>
   *   <li>If {@code credentials} is provided but the password is {@code null}, no authentication
   *       is performed.</li>
   * </ul>
   *
   * <p>Note:</p>
   * <ul>
   *   <li>{@code HELLO} is available only in Redis 6.0 and newer.</li>
   *   <li>The command both negotiates the protocol version (RESP2/RESP3) and returns
   *       server metadata.</li>
   * </ul>
   *
   * @param protocol the requested RESP protocol version (must not be {@code null})
   * @param credentials optional credentials used for authentication (may be {@code null})
   * @return the parsed {@code HELLO} response containing server metadata
   * @throws IllegalArgumentException if {@code protocol} is {@code null}
   */
  HelloResult hello(RedisProtocol protocol, RedisCredentials credentials) {
    if (protocol == null) {
      throw new IllegalArgumentException("protocol must not be null");
    }

    byte[] rawPass = null;
    try {
      byte[][] args;
      byte[] versionRaw = encode(protocol.version());
      if (credentials != null && credentials.getPassword() != null) {
        String user = credentials.getUser();
        if (user == null) {
          user = "default";
        }
        rawPass = encodeToBytes(credentials.getPassword());
        args = new byte[][] { versionRaw, Keyword.AUTH.getRaw(), encode(user), rawPass };
      } else {
        args = new byte[][] { versionRaw };
      }
      return new HelloResult(hello(args));
    } finally {
      if (rawPass != null) {
        Arrays.fill(rawPass, (byte) 0); // clear sensitive data
      }
    }
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
   * @return the list of push consumers
   */
  List<PushConsumer> getPushConsumers() {
    return pushConsumers.getConsumers();
  }

  @Internal
  protected void addPushConsumer(PushConsumer consumer) {
    this.pushConsumers.add(consumer);
  }

  @Internal
  protected void removePushConsumer(PushConsumer consumer) {
    this.pushConsumers.remove(consumer);
  }

  /**
   * {@code true} while this connection is operating with relaxed timeouts because a server-side
   * maintenance event is in progress on it (MIGRATING / FAILING_OVER, or a MOVING handoff
   * delivered on this connection). Reverts to {@code false} once the event completes or the
   * relaxation window expires.
   */
  boolean isRelaxedTimeoutActive() {
    long d = relaxedUntilNanos;
    if (d == 0) return false;
    if (d - clockNanos.getAsLong() > 0) return true;
    relaxedUntilNanos = 0; // lazy reset past deadline; same thread owns the field
    return false;
  }

  /**
   * The {@code SO_TIMEOUT} to apply for the next operation. Consults the {@link #soTimeoutSupplier}
   * first (e.g. a pool-injected MOVING-rebind override); when it has no opinion
   * ({@link JedisClientConfig#UNSET_TIMEOUT_MS}) or is absent, falls back to this connection's own
   * calculation based on its blocking mode and per-receiver relaxation window.
   */
  private int effectiveSoTimeout() {
    if (soTimeoutSupplier != null) {
      int v = soTimeoutSupplier.getSoTimeout(isBlocking);
      if (JedisClientConfig.isTimeoutSet(v)) {
        return v;
      }
    }
    return desiredSoTimeout(isBlocking, isRelaxedTimeoutActive());
  }

  /**
   * Returns the {@code SO_TIMEOUT} value to apply for the next operation given the blocking mode
   * and whether relaxation is in effect right now.
   */
  private int desiredSoTimeout(boolean blocking, boolean relaxed) {
    if (blocking) {
      return relaxed && relaxedBlockingTimeoutConfigured ? relaxedBlockingTimeout : infiniteSoTimeout;
    }
    return relaxed && relaxedTimeoutConfigured ? relaxedTimeout : soTimeout;
  }

  /**
   * Sets {@code SO_TIMEOUT} only when {@code desired} differs from what we've previously applied —
   * skips the {@code setSoTimeout} syscall in the steady state.
   */
  private void applyTimeout(int desired) {
    if (socket == null || lastAppliedTimeout == desired) {
      return;
    }
    try {
      socket.setSoTimeout(desired);
      lastAppliedTimeout = desired;
    } catch (SocketException ex) {
      setBroken();
      throw new JedisConnectionException(ex);
    }
  }

  /**
   * Switches this connection to relaxed timeouts for at most {@code period}. While the window is
   * open, commands use {@link #getRelaxedSoTimeout()} and {@link #getRelaxedBlockingSoTimeout()}
   * instead of the configured values, giving in-flight commands extra headroom across a
   * server-side maintenance event (MIGRATING / FAILING_OVER / MOVING-receiver). The original
   * timeouts return into effect once the window closes or {@link #resetRelaxedTimeouts()} is
   * called. Calling this with a later deadline extends the window; an earlier one is ignored.
   *
   * @param period maximum duration of the relaxation window
   */
  void relaxTimeouts(Duration period) {
    long deadline = clockNanos.getAsLong() + period.toNanos();
    if (relaxedUntilNanos == 0 || deadline - relaxedUntilNanos > 0) {
      relaxedUntilNanos = deadline;
    }
    applyTimeout(effectiveSoTimeout());
  }

  /** Clears the per-receiver relaxation deadline and eagerly realigns the socket (cache-checked). */
  void resetRelaxedTimeouts() {
    relaxedUntilNanos = 0;
    applyTimeout(effectiveSoTimeout());
  }

  void setClockNanos(LongSupplier clockNanos) {
    this.clockNanos = clockNanos;
  }

  /** Marks this connection for discard on return to the pool (MOVING receiver). */
  void requestRebind() {
    this.rebindRequested = true;
  }

  /** The connected peer's address, or {@code null} if the socket is not (yet) open. */
  SocketAddress getRemoteSocketAddress() {
    return socket == null ? null : socket.getRemoteSocketAddress();
  }

  /** Registers a listener notified synchronously of this connection's maintenance events. */
  void addMaintenanceEventListener(MaintenanceEventListener listener) {
    maintenanceEventListeners.add(listener);
  }

  /** Removes a previously registered maintenance event listener. */
  void removeMaintenanceEventListener(MaintenanceEventListener listener) {
    maintenanceEventListeners.remove(listener);
  }

}
