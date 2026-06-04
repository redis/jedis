package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.authentication.JedisAuthenticationException;
import redis.clients.jedis.authentication.AuthXEventListener;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisException;

/**
 * PoolableObjectFactory custom impl.
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> , RebindAware {

  public static class Builder {
    private JedisClientConfig clientConfig;
    private Connection.Builder connectionBuilder;
    private JedisSocketFactory jedisSocketFactory;
    private Cache cache;
    private HostAndPort hostAndPort;

    // Fluent API methods (preferred)
    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public Builder connectionBuilder(Connection.Builder connectionBuilder) {
      this.connectionBuilder = connectionBuilder;
      return this;
    }

    public Builder socketFactory(JedisSocketFactory jedisSocketFactory) {
      this.jedisSocketFactory = jedisSocketFactory;
      return this;
    }

    public Builder cache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public Builder hostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    public Connection.Builder getConnectionBuilder() {
      return connectionBuilder;
    }

    public JedisSocketFactory getJedisSocketFactory() {
      return jedisSocketFactory;
    }

    public JedisClientConfig getClientConfig() {
      return clientConfig;
    }

    public Cache getCache() {
      return cache;
    }

    public ConnectionFactory build() {
      withDefaults();
      return new ConnectionFactory(this);
    }

    private Builder withDefaults() {
      if (jedisSocketFactory == null) {
        this.jedisSocketFactory = createDefaultSocketFactory();
      }
      if (connectionBuilder == null) {
        this.connectionBuilder = createDefaultConnectionBuilder();
      }
      return this;
    }

    private JedisSocketFactory createDefaultSocketFactory() {
      if (clientConfig == null) {
        clientConfig = DefaultJedisClientConfig.builder().build();
      }
      if (hostAndPort == null) {
        throw new IllegalStateException("HostAndPort is required when no socketFactory is provided");
      }
      return new DefaultJedisSocketFactory(hostAndPort, clientConfig);
    }

    private Connection.Builder createDefaultConnectionBuilder() {
      Connection.Builder connBuilder = cache == null ? Connection.builder() : CacheConnection.builder(cache);
      connBuilder.socketFactory(jedisSocketFactory).clientConfig(clientConfig);
      return connBuilder;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisClientConfig clientConfig;
  private Supplier<Connection> objectMaker;
  private Connection.Builder connectionBuilder;

  private AuthXEventListener authXEventListener;

  /** Active MOVING rebind, or {@code null}. Read for target selection (socket factory) and to relax connections created mid-window (makeObject). */
  private final AtomicReference<RebindState> rebindState = new AtomicReference<>();
  private LongSupplier clockNanos = System::nanoTime;

  /** Immutable snapshot of an active, time-bounded MOVING rebind. */
  private static final class RebindState {
    private final long seq;
    private final HostAndPort target;
    private final long deadlineNanos;

    RebindState(long seq, HostAndPort target, long deadlineNanos) {
      this.seq = seq;
      this.target = target;
      this.deadlineNanos = deadlineNanos;
    }
  }

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this(builder().hostAndPort(hostAndPort).withDefaults());
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(builder().hostAndPort(hostAndPort).clientConfig(clientConfig).withDefaults());
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig, Cache csCache) {
    this(builder().hostAndPort(hostAndPort).clientConfig(clientConfig).cache(csCache).withDefaults());
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    this(builder().socketFactory(jedisSocketFactory).clientConfig(clientConfig).withDefaults());
  }

  public ConnectionFactory(Builder builder) {
    this.clientConfig = builder.getClientConfig();
    this.connectionBuilder = builder.getConnectionBuilder();

    // The socket factory resolves its target through our rebind overlay: override during the grace
    // window, else null (falls back to its configured host). Pure read → race-free, revert implicit.
    JedisSocketFactory socketFactory = connectionBuilder.getSocketFactory();
    if (socketFactory instanceof DefaultJedisSocketFactory) {
      ((DefaultJedisSocketFactory) socketFactory).setHostAndPortSupplier(() -> {
        RebindState s = rebindState.get();
        return (s != null && s.deadlineNanos - clockNanos.getAsLong() > 0) ? s.target : null;
      });
    }

    initAuthXManager();
  }

  private void initAuthXManager() {
    AuthXManager authXManager = clientConfig.getAuthXManager();
    if (authXManager == null) {
      this.objectMaker = () -> build();
      this.authXEventListener = AuthXEventListener.NOOP_LISTENER;
    } else {
      this.objectMaker = () -> (Connection) authXManager.addConnection(build());
      this.authXEventListener = authXManager.getListener();
      authXManager.start();
    }
  }

  private Connection build() {
    return connectionBuilder.build();
  }

  @Override
  public void activateObject(PooledObject<Connection> pooledConnection) throws Exception {
    // what to do ??
  }

  @Override
  public void destroyObject(PooledObject<Connection> pooledConnection) throws Exception {
    final Connection jedis = pooledConnection.getObject();
    if (jedis.isConnected()) {
      try {
        jedis.close();
      } catch (RuntimeException e) {
        logger.debug("Error while close", e);
      }
    }
  }

  @Override
  public PooledObject<Connection> makeObject() throws Exception {
    try {
      Connection jedis = objectMaker.get();
      return new DefaultPooledObject<>(jedis);
    } catch (JedisException je) {
      logger.debug("Error while makeObject", je);
      throw je;
    }
  }

  @Override
  public void passivateObject(PooledObject<Connection> pooledConnection) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
    Connection jedis = pooledConnection.getObject();
    reAuthenticate(jedis);
  }

  @Override
  public boolean validateObject(PooledObject<Connection> pooledConnection) {
    final Connection jedis = pooledConnection.getObject();
    try {
      // check HostAndPort ??
      if (!jedis.isConnected()) {
        return false;
      }
      reAuthenticate(jedis);
      return jedis.ping();
    } catch (final Exception e) {
      logger.warn("Error while validating pooled Connection object.", e);
      return false;
    }
  }

  private void reAuthenticate(Connection jedis) throws Exception {
    try {
      String result = jedis.reAuthenticate();
      if (result != null && !result.equals("OK")) {
        String msg = "Re-authentication failed with server response: " + result;
        Exception failedAuth = new JedisAuthenticationException(msg);
        logger.error(failedAuth.getMessage(), failedAuth);
        authXEventListener.onConnectionAuthenticationError(failedAuth);
        return;
      }
    } catch (Exception e) {
      logger.error("Error while re-authenticating connection", e);
      authXEventListener.onConnectionAuthenticationError(e);
      throw e;
    }
  }


  /**
   * Records a {@code ttlSeconds}-bounded target override, applied only when {@code seq} is newer
   * than the last — duplicate and out-of-order MOVINGs are ignored. New connections resolve to
   * {@code newHostAndPort} during the window, then to the configured host once it expires.
   * Lock-free; target selection reads this state, so it never observes a stale target.
   */
  @Override
  public RebindResult rebind(long seq, HostAndPort newHostAndPort, long ttlSeconds) {
    long deadline = clockNanos.getAsLong() + TimeUnit.SECONDS.toNanos(ttlSeconds);
    while (true) {
      RebindState cur = rebindState.get();
      if (cur != null && seq <= cur.seq) {
        return RebindResult.STALE; // duplicate or out-of-order event
      }
      RebindState next = new RebindState(seq, newHostAndPort, deadline);
      if (rebindState.compareAndSet(cur, next)) {
        logger.debug("Rebinding to {} (seq={}, ttl={}s)", newHostAndPort, seq, ttlSeconds);
        return RebindResult.APPLIED_NEW_TARGET;
      }
      // Lost the CAS to a concurrent apply; retry (may then observe STALE).
    }
  }

  /** Test seam: override the monotonic clock used for rebind expiry. */
  void setClockNanos(LongSupplier clockNanos) {
    this.clockNanos = clockNanos;
  }

}
