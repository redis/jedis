package redis.clients.jedis;

import static redis.clients.jedis.PushConsumerChain.PROPAGATE_ALL_HANDLER;
import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class Connection implements Closeable {
  public static Logger logger = LoggerFactory.getLogger(Connection.class);

  private ConnectionPool memberOf;
  protected RedisProtocol protocol;
  private final JedisSocketFactory socketFactory;
  private Socket socket;
  private RedisOutputStream outputStream;
  private RedisInputStream inputStream;
  private boolean relaxedTimeoutEnabled = false;
  private int relaxedTimeout = safeToInt(TimeoutOptions.DISABLED_TIMEOUT.toMillis());
  private int relaxedBlockingTimeout = safeToInt(TimeoutOptions.DISABLED_TIMEOUT.toMillis());
  private int soTimeout = 0;
  private int infiniteSoTimeout = 0;
  private boolean broken = false;
  private boolean strValActive;
  private String strVal;
  protected String server;
  protected String version;
  private AtomicReference<RedisCredentials> currentCredentials = new AtomicReference<>(null);
  private AuthXManager authXManager;
  private boolean isBlocking = false;
  private boolean isRelaxed = false;
  private boolean rebindRequested = false;

  protected PushConsumerChain pushConsumer;
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
    this.soTimeout = clientConfig.getSocketTimeoutMillis();
    this.infiniteSoTimeout = clientConfig.getBlockingSocketTimeoutMillis();
    this.relaxedTimeout = safeToInt(clientConfig.getTimeoutOptions().getRelaxedTimeout().toMillis());
    this.relaxedBlockingTimeout = safeToInt(clientConfig.getTimeoutOptions().getRelaxedBlockingTimeout().toMillis());
    this.relaxedTimeoutEnabled =  TimeoutOptions.isRelaxedTimeoutEnabled(relaxedTimeout) ||
        TimeoutOptions.isRelaxedTimeoutEnabled(relaxedBlockingTimeout);
    initPushConsumers(clientConfig);
    initializeFromClientConfig(clientConfig);
  }


  protected void initPushConsumers(JedisClientConfig config) {
    /*
     * Default consumers to process push messages.
     * Marks all @{link PushMessage}s as processed, except for pub/sub.
     * Pub/sub messages are propagated to the client.
     */
    this.pushConsumer = PushConsumerChain.of(
        PushConsumerChain.CONSUME_ALL_HANDLER,
        PushConsumerChain.PUBSUB_ONLY_HANDLER
    );

    if (config != null) {

      /*
       * Add consumer to handle server maintenance events.
       * Maintenance events are propagated to the registered {@link MaintenanceEventListener}s.
       */
      MaintenanceEventHandler maintenanceEventHandler = config.getMaintenanceEventHandler();
      if (maintenanceEventHandler != null) {
        this.pushConsumer.add(new MaintenanceEventConsumer(maintenanceEventHandler));

        if (config.isProactiveRebindEnabled()) {
          maintenanceEventHandler.addListener(new ConnectionRebindHandler());
        }

        if (TimeoutOptions.isRelaxedTimeoutEnabled(config.getTimeoutOptions().getRelaxedTimeout())) {
          maintenanceEventHandler.addListener(new AdaptiveTimeoutHandler(Connection.this));
        }
      }

      /*
       * Add consumer to notify registered {@link PushListener}s.
       */
      PushHandler pushHandler = config.getPushHandler();
      if (pushHandler != null) {
        this.pushConsumer.add(new ListenerNotificationConsumer(pushHandler));
      }
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
      int timeout = getDesiredTimeout();
      socket.setSoTimeout(timeout);
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
    final byte[] resp = (byte[]) readProtocolWithCheckingBroken(pushConsumer);
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
    return (byte[]) readProtocolWithCheckingBroken(pushConsumer);
  }

  public Long getIntegerReply() {
    flush();
    return (Long) readProtocolWithCheckingBroken(pushConsumer);
  }

  public List<String> getMultiBulkReply() {
    return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
  }

  @SuppressWarnings("unchecked")
  public List<byte[]> getBinaryMultiBulkReply() {
    flush();
    return (List<byte[]>) readProtocolWithCheckingBroken(pushConsumer);
  }

  /**
   * @deprecated Use {@link Connection#getUnflushedObject()}.
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public List<Object> getUnflushedObjectMultiBulkReply() {
    return (List<Object>) readProtocolWithCheckingBroken(pushConsumer);
  }

  @SuppressWarnings("unchecked")
  public Object getUnflushedObject() {
    return readProtocolWithCheckingBroken(pushConsumer);
  }

  public List<Object> getObjectMultiBulkReply() {
    flush();
    return (List<Object>) readProtocolWithCheckingBroken(pushConsumer);
  }

  @SuppressWarnings("unchecked")
  public List<Long> getIntegerMultiBulkReply() {
    flush();
    return (List<Long>) readProtocolWithCheckingBroken(pushConsumer);
  }

  public Object getOne() {
    flush();
    return readProtocolWithCheckingBroken(pushConsumer);
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
  protected Object protocolRead(RedisInputStream is, PushConsumer handler) {
    return Protocol.read(is, handler);
  }

  @Experimental
  protected void protocolReadPushes(RedisInputStream is) {
  }

  protected Object readProtocolWithCheckingBroken(PushConsumer handler) {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection.");
    }

    try {
      return protocolRead(inputStream, handler);
    } catch (JedisConnectionException exc) {
      broken = true;
      throw exc;
    }
  }

  /**
   * @deprecated Use {@link #readProtocolWithCheckingBroken(PushConsumer)}
   * @return
   */
  @Deprecated
  protected Object readProtocolWithCheckingBroken() {
    if (broken) {
      throw new JedisConnectionException("Attempting to read from a broken connection.");
    }

    try {
      return protocolRead(inputStream, PROPAGATE_ALL_HANDLER);
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
        responses.add(readProtocolWithCheckingBroken(pushConsumer));
      } catch (JedisDataException e) {
        responses.add(e);
      }
    }
    return responses;
  }

  /**
   * Check if the client name libname, libver, characters are legal
   *
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

  protected void initializeFromClientConfig(final JedisClientConfig config) {
    try {
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

  @Experimental
  @VisibleForTesting
  PushConsumerChain getPushConsumer() {
    return this.pushConsumer;
  }

  @Experimental
  public boolean isRelaxedTimeoutActive() {
    return isRelaxed;
  }

  /**
   * Calculate the desired timeout based on current state (blocking/non-blocking and relaxed/normal).
   * When relaxed timeouts are enabled, use configured relaxed timeout if available, otherwise fallback to default timeout.
   */
  private int getDesiredTimeout() {
    if (!isRelaxed) {
      if (!isBlocking) {
        return soTimeout;
      } else {
        return infiniteSoTimeout;
      }
    } else {
      if (!isBlocking) {
        // Use relaxed timeout if configured, otherwise fallback to normal timeout
        return TimeoutOptions.isRelaxedTimeoutDisabled(relaxedTimeout) ? soTimeout : relaxedTimeout;
      } else {
        // Use relaxed blocking timeout if configured, otherwise fallback to infinite timeout
        return TimeoutOptions.isRelaxedTimeoutDisabled(relaxedBlockingTimeout) ? infiniteSoTimeout : relaxedBlockingTimeout;
      }
    }
  }

  @Experimental
  public void relaxTimeouts() {
    if (!relaxedTimeoutEnabled) {
      return;
    }

    if (!isRelaxed) {
      isRelaxed = true;
      try {
        if (isConnected()) {
          socket.setSoTimeout(getDesiredTimeout());
        }
      } catch (SocketException ex) {
        setBroken();
        throw new JedisConnectionException(ex);
      }
    }
  }

  @Experimental
  public void disableRelaxedTimeout() {
    if (isRelaxed) {
      isRelaxed = false;
      try {
        if (isConnected()) {
          socket.setSoTimeout(getDesiredTimeout());
        }
      } catch (SocketException ex) {
        setBroken();
        throw new JedisConnectionException(ex);
      }
    }
  }

  private static int safeToInt(long millis) {
    if (millis > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }

    return (int) millis;
  }
  /**
   * Push consumer that delegates to a {@link PushHandler} for listener notification.
   */
  private static class ListenerNotificationConsumer implements PushConsumer {
    private final PushHandler pushHandler;

    public ListenerNotificationConsumer(PushHandler pushHandler) {
      this.pushHandler = pushHandler;
    }

    @Override
    public void accept(PushConsumerContext context) {
      if (pushHandler != null) {
        notifyListeners(context.getMessage());
      }
    }

    private void notifyListeners(PushMessage pushMessage) {
      try {
        pushHandler.getPushListeners().forEach(pushListener -> {
          try {
            pushListener.onPush(pushMessage);
          } catch (Exception e) {
            // ignore
          }
        });
      } catch (Exception e) {
        // Log notification failures
      }
    }
  }


  private static class MaintenanceEventConsumer implements PushConsumer {
    private final MaintenanceEventHandler eventHandler;

    public MaintenanceEventConsumer(MaintenanceEventHandler eventHandler) {
      this.eventHandler = eventHandler;
    }

    @Override
    public void accept(PushConsumerContext context) {
      PushMessage message = context.getMessage();

      switch ( message.getType()) {
      case "MOVING":
        onMoving(message);
        break;
      case "MIGRATING":
        onMigrating();
        break;
      case "MIGRATED":
        onMigrated();
        break;
      case "FAILING_OVER":
        onFailOver();
        break;
      case "FAILED_OVER":
        onFailedOver();
        break;
      }
    }
    private void onMoving(PushMessage message) {
      HostAndPort rebindTarget = getRebindTarget(message);
      eventHandler.getListeners().forEach(listener -> listener.onRebind(rebindTarget));
    }

    private void onMigrating() {
      eventHandler.getListeners().forEach(MaintenanceEventListener::onMigrating);
    }

    private void onMigrated() {
      eventHandler.getListeners().forEach(MaintenanceEventListener::onMigrated);
    }

    private void onFailOver() {
      eventHandler.getListeners().forEach(MaintenanceEventListener::onFailOver);
    }

    private void onFailedOver() {
      eventHandler.getListeners().forEach(MaintenanceEventListener::onFailedOver);
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
        logger.warn("Invalid re-bind message format, expected 3rd element to be a byte[], got {}",
            addressObject.getClass());
        return null;
      }

      try {
        String addressAndPort = SafeEncoder.encode((byte[]) addressObject);
        String[] parts = addressAndPort.split(":");
        if (parts.length != 2) {
          logger.warn("Invalid re-bind message format, expected 'host:port', got {}",
              addressAndPort);
          return null;
        }

        String address = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new HostAndPort(address, port);
      } catch (Exception e) {
        logger.warn("Error parsing re-bind target from message: {}", message, e);
        return null;
      }
    }
  }

  private class ConnectionRebindHandler implements MaintenanceEventListener {
      public void onRebind(HostAndPort target) {
        rebindRequested = true;
      }
  }

  private static class AdaptiveTimeoutHandler implements MaintenanceEventListener {

    private final WeakReference<Connection> connectionRef;

    /**
     * Creates a new maintenance listener for the specified connection.
     *
     * @param connection The connection to manage timeouts for
     */
    public AdaptiveTimeoutHandler(Connection connection) {
      this.connectionRef = new WeakReference<>(connection);
    }

    public void onMigrating() {
      Connection connection = connectionRef.get();
      if (connection != null) {
        connection.relaxTimeouts();
      }
    }

    public void onMigrated() {
      Connection connection = connectionRef.get();
      if (connection != null) {
        connection.disableRelaxedTimeout();
      }
    }

    public void onFailOver() {
      Connection connection = connectionRef.get();
      if (connection != null) {
        connection.relaxTimeouts();
      }
    }

    public void onFailedOver() {
      Connection connection = connectionRef.get();
      if (connection != null) {
        connection.disableRelaxedTimeout();
      }
    }

    public void onRebind(HostAndPort target) {
      Connection connection = connectionRef.get();
      if (connection != null) {
        connection.relaxTimeouts();
      }
    }
  }
}
