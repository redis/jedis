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
  private final ProtocolHandshake handshake = new ProtocolHandshake(this);

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
}
