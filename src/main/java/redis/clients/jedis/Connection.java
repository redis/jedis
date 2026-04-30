package redis.clients.jedis;

import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.util.Collections;
import java.util.logging.Logger;
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

  private static final Logger logger = Logger.getLogger(Connection.class.getName());

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
  /**
   * Stores the requested protocol configuration for this connection.
   *
   * <p>Defines the desired RESP protocol behavior for the client.</p>
   *
   * <ul>
   *   <li>{@code null} – No explicit protocol requested. The connection will not perform
   *       protocol negotiation and will assume RESP2 as the default server protocol.</li>
   *   <li>{@code RESP2} – Enforce RESP2 via HELLO negotiation (no fallback).</li>
   *   <li>{@code RESP3} – Enforce RESP3 via HELLO negotiation (no fallback).</li>
   *   <li>{@code RESP3_PREFERRED} – Attempt RESP3, fallback to RESP2 if not supported.</li>
   * </ul>
   */
  protected RedisProtocol protocol;

  /**
   * The protocol version established with the server.
   *
   * <p>This value is set after a successful handshake and reflects the actual RESP protocol
   * used for communication with the server.</p>
   *
   * <p>May be {@code null} if the handshake has not yet been performed.</p>
   */
  private RespProtocol establishedProtocol;
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
          establishedProtocol = establishProtocol(requestedProtocol, redisCredentialsProvider.get());
        } finally {
          redisCredentialsProvider.cleanUp();
        }
      } else {
        establishedProtocol = establishProtocol(requestedProtocol, credentialsProvider != null ? credentialsProvider.get()
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

  // TODO: Move to Top level class and Builder
  static class HelloResult {
    final Map<String, Object> helloResponse;

    HelloResult(Map<String, Object> helloResponse) {
      this.helloResponse = helloResponse;
    }

    /**
     * @return the protocol version the server actually accepted
     */
    public RespProtocol getProtocol() {
      Long proto = (Long) helloResponse.get("proto");
      return RespProtocol.of(proto);
    }

    public String getServer() {
      return (String) helloResponse.get("server");
    }

    public String getVersion() {
      return (String) helloResponse.get("version");
    }
  }

  /**
   * Send HELLO command to the server to negotiate the protocol version and authenticate if needed.
   * <p>
   * Attempts RESP3 handshake, falls back to RESP2 if not supported.
   * </p>
   * @param credentials credentials for authentication
   * @return {@link RespProtocol} the actual negotiated protocol version
   */
  private HelloResult negotiateResp3WithFallback(final RedisCredentials credentials){
    try {
      return enforceProtocolWithAuth(RespProtocol.RESP3, credentials);
    } catch (JedisProtocolNotSupportedException e) {
      // fall back to resp2
      return establishLegacyResp2(credentials);
    } catch (JedisDataException e) {
      // fall back to resp2
      if (isUnknownCommandError(e)) {
        return establishLegacyResp2(credentials);
      }
      throw e;
    }
  }

  /**
   * Performs strict protocol negotiation using the {@code HELLO} command.
   *
   * <p>This method enforces the provided {@code protocol} version and expects the server
   * to support the {@code HELLO} command. It does not perform protocol fallback
   * (e.g., RESP3 → RESP2).</p>
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>Attempts negotiation via {@code HELLO <protocol>} without authentication.</li>
   *   <li>If the server rejects the request with a NOAUTH error (observed in Redis 6.0.x
   *       prior to 6.2.2), performs an {@code AUTH} using the provided credentials and
   *       retries the handshake.</li>
   *   <li>Any non-authentication-related errors are propagated to the caller.</li>
   * </ul>
   *
   * <p>Notes:</p>
   * <ul>
   *   <li>Some Redis 6.0.x versions require authentication before allowing {@code HELLO},
   *       even though {@code HELLO AUTH} is supported in later versions.</li>
   *   <li>This method assumes the server supports the requested protocol; unsupported
   *       protocol errors are not handled and will be propagated.</li>
   * </ul>
   *
   * @param protocol the RESP protocol version to negotiate (must not be {@code null})
   * @param credentials credentials used for authentication if required (may be {@code null})
   * @return the {@code HELLO} response containing negotiated protocol and server metadata
   * @throws IllegalArgumentException if {@code protocol} is {@code null}
   * @throws JedisProtocolNotSupportedException if the server does not support the requested protocol
   * @throws JedisAccessControlException if authentication fails and cannot be recovered
   */
  private HelloResult enforceProtocolWithAuth(RespProtocol protocol, RedisCredentials credentials) {
    if (protocol == null) {
      throw new IllegalArgumentException("protocol must not be null");
    }

    try {
      try {
        return helloCommand(protocol, null);
      } catch (JedisDataException e) {
        if (isUnknownCommandError(e)) {
          throw new JedisProtocolNotSupportedException("Server does not support HELLO", e);
        } else {
          throw e;
        }
      }
    } catch (JedisAccessControlException e) {
      // Redis 6.0.x (before 6.2.2) has a bug where HELLO with AUTH fails if the default user
      // requires authentication — the server demands AUTH before allowing HELLO.
      // See: https://github.com/redis/redis/issues/8558
      // See: https://github.com/redis/lettuce/issues/2592
      if (isNoAuthError(e)) {
        authenticate(credentials);
        return helloCommand(protocol, credentials);
      } else {
        throw e;
      }
    }

  }

  /**
   * Fallback handshake used when RESP3 or {@code HELLO} is not supported by the server.
   *
   * <p>This method provides compatibility with legacy Redis servers that do not support
   * the {@code HELLO} command.</p>
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>Performs {@code AUTH} if credentials are provided.</li>
   *   <li>Attempts a {@code HELLO 2} command to retrieve server metadata.</li>
   *   <li>If the server does not support {@code HELLO}, assumes RESP2 as the default protocol.</li>
   * </ul>
   *
   * <p>Fallback logic:</p>
   * <ul>
   *   <li>If {@code HELLO} succeeds → uses returned protocol and metadata.</li>
   *   <li>If {@code HELLO} fails with unknown command → assumes RESP2 and continues.</li>
   *   <li>Any other errors are propagated to the caller.</li>
   * </ul>
   *
   * <p>Note:</p>
   * <ul>
   *   <li>This method exists solely for backward compatibility with Redis versions prior to 6.0.</li>
   *   <li>Server version and protocol information may be incomplete when fallback is used.</li>
   * </ul>
   *
   * @param credentials credentials used for authentication (may be {@code null})
   * @return {@link HelloResult} containing protocol and server metadata (may be inferred for legacy servers)
   * @throws JedisDataException if a non-recoverable server error occurs
   */
  private HelloResult establishLegacyResp2(final RedisCredentials credentials) {
    // authenticate first to support legacy behavior on server not supporting HELLO
    authenticate(credentials);

    try {
      return helloCommand(RespProtocol.RESP2, null);
    } catch (JedisDataException e) {
      // if server does not support hello, we assume RESP2
      if (isUnknownCommandError(e)) {
        return new HelloResult(Collections.singletonMap("proto", Long.valueOf(RespProtocol.RESP2.version())));
      }

      throw e;
    }
  }


  boolean isNoAuthError(JedisDataException e){
    return e.getMessage().startsWith("NOAUTH");
  }

  boolean isUnknownCommandError(JedisDataException e){
    return e.getMessage().startsWith("ERR") && e.getMessage().contains("unknown command");
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
   *   <li>If {@code requestedProtocol} is {@code null}, no {@code HELLO} is sent.
   *       The connection assumes RESP2 as the default protocol and only {@code AUTH}
   *       is performed if credentials are provided.</li>
   *
   *   <li>If {@code RESP2} is requested, a strict {@code HELLO 2} handshake is performed.</li>
   *
   *   <li>If {@code RESP3} is requested, a strict {@code HELLO 3} handshake is performed.</li>
   *
   *   <li>If {@code RESP3_PREFERRED} is requested, the client first attempts
   *       {@code HELLO 3} and falls back to RESP2 if RESP3 is not supported.</li>
   * </ul>
   *
   * <p>Legacy behavior:</p>
   * <ul>
   *   <li>When no protocol is explicitly configured, the client assumes RESP2 without
   *       performing protocol negotiation.</li>
   * </ul>
   *
   * <p>Side effects:</p>
   * <ul>
   *   <li>Performs authentication if credentials are provided.</li>
   *   <li>Populates internal server metadata (version, server info).</li>
   *   <li>Stores the resolved protocol version for the connection.</li>
   * </ul>
   *
   * @param requestedProtocol the requested RESP protocol (may be {@code null} for legacy mode)
   * @param credentials credentials used for authentication (may be {@code null})
   * @return the resolved {@link RespProtocol} negotiated for this connection
   * @throws IllegalArgumentException if the requested protocol is not supported
   * @throws JedisProtocolNotSupportedException if protocol negotiation fails
   * @throws JedisDataException if the server returns an error during handshake
   */
  private RespProtocol establishProtocol(final RedisProtocol requestedProtocol, final RedisCredentials credentials) {

    this.protocol = requestedProtocol;
    boolean noProtocolRequested = this.protocol == null;

    HelloResult helloResult;
    // This is needed to keep the compatibility with legacy Jedis class and
    // avoid sending hello command when user haven't provided any protocol version and credentials.
    // if no protocol requested we assume server default is RESP2,
    // and configure connection to expect RESP2
    if (noProtocolRequested) {
      authenticate(credentials);
      helloResult = new HelloResult(Collections.singletonMap("proto", Long.valueOf(RespProtocol.RESP2.version())));
    } else if (requestedProtocol == RedisProtocol.RESP2) {
      helloResult = enforceProtocolWithAuth(RespProtocol.RESP2, credentials);
    } else if (requestedProtocol == RedisProtocol.RESP3) {
      helloResult = enforceProtocolWithAuth(RespProtocol.RESP3, credentials);
    } else if (requestedProtocol == RedisProtocol.RESP3_PREFERRED) {
      helloResult = negotiateResp3WithFallback(credentials);
    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + requestedProtocol);
    }

    version = helloResult.getVersion();
    server = helloResult.getServer();

    return helloResult.getProtocol();
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
  private HelloResult helloCommand(RespProtocol protocol, RedisCredentials credentials) {
    if (protocol == null) {
      throw new IllegalArgumentException("protocol must not be null");
    }

    CommandArguments helloCmd = new CommandArguments(Command.HELLO).add(protocol.versionRaw());
    if (credentials != null) {
      if (credentials.getPassword() != null) {

        String user = credentials.getUser();
        if (user == null) {
          user = "default";
        }

        byte[] rawPass = encodeToBytes(credentials.getPassword());

        try {
          helloCmd.add(Keyword.AUTH).add(encode(user)).add(rawPass);
        } finally {
          Arrays.fill(rawPass, (byte) 0); // clear sensitive data
        }
      }
    }

    sendCommand(helloCmd);
    Map<String, Object> response = BuilderFactory.ENCODED_OBJECT_MAP.build(getOne());
    return new HelloResult(response);
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
  public RespProtocol getEstablishedProtocol() {
    return establishedProtocol;
  }
}
