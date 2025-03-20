package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
public class ConnectionFactory implements PooledObjectFactory<Connection> {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

  private final JedisSocketFactory jedisSocketFactory;
  private final JedisClientConfig clientConfig;
  private final Cache clientSideCache;
  private final Supplier<Connection> objectMaker;

  private final AuthXEventListener authXEventListener;
  /**
   * Only one connection is maintained between a client and a server node for tracking and receiving invalidation messages.
   * <p>
   * This is done to avoid the server sending duplicate messages to multiple connections,
   * thereby reducing the CPU consumption of the server.
   */
  private CacheConnection trackingConnection = null;

  /**
   * The single thread executor for listening invalidation messages.
   */
  private ScheduledExecutorService invalidationListeningExecutor = null;

  public ConnectionFactory(final HostAndPort hostAndPort) {
    this(hostAndPort, DefaultJedisClientConfig.builder().build(), null);
  }

  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig) {
    this(hostAndPort, clientConfig, null);
  }

  @Experimental
  public ConnectionFactory(final HostAndPort hostAndPort, final JedisClientConfig clientConfig,
      Cache csCache) {
    this(new DefaultJedisSocketFactory(hostAndPort, clientConfig), clientConfig, csCache);
    if (!clientConfig.getTrackingConfig().isTrackingModeOnDefault()) {
      invalidationListeningExecutor = Executors.newSingleThreadScheduledExecutor();
      // initialize tracking connection
      initializeTrackingConnection();
    }
  }

  public ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig) {
    this(jedisSocketFactory, clientConfig, null);
  }

  private ConnectionFactory(final JedisSocketFactory jedisSocketFactory,
      final JedisClientConfig clientConfig, Cache csCache) {

    this.jedisSocketFactory = jedisSocketFactory;
    this.clientSideCache = csCache;
    this.clientConfig = clientConfig;

    AuthXManager authXManager = clientConfig.getAuthXManager();
    if (authXManager == null) {
      this.objectMaker = connectionSupplier();
      this.authXEventListener = AuthXEventListener.NOOP_LISTENER;
    } else {
      Supplier<Connection> supplier = connectionSupplier();
      this.objectMaker = () -> (Connection) authXManager.addConnection(supplier.get());
      this.authXEventListener = authXManager.getListener();
      authXManager.start();
    }
  }

  /**
   * Create a "tracking" connection and start tracking and listen invalidation messages periodically.
   */
  @Experimental
  private void initializeTrackingConnection() {
    trackingConnection = new CacheConnection(jedisSocketFactory, clientConfig, clientSideCache);
    tracking();
    startInvalidationListenerThread();
  }

  /**
   * Tracking on broadcasting mode.
   */
  @Experimental
  private void tracking() {
    List<String> trackingPrefixList = clientConfig.getTrackingConfig().getTrackingPrefixList();
    // if no prefix is set, the prefix is "".
    if (trackingPrefixList == null) {
      trackingPrefixList = new ArrayList<>();
      trackingPrefixList.add("");
    }
    trackingConnection.sendCommandWithTracking(Protocol.Command.CLIENT, trackingPrefixList, "TRACKING", "ON", "BCAST");
    String reply = trackingConnection.getStatusCodeReply();
    if (!"OK".equals(reply)) {
      throw new JedisException("Could not enable client tracking. Reply: " + reply);
    }
  }

  /**
   * Start a scheduled task to listen for invalidation event.
   */
  @Experimental
  private void startInvalidationListenerThread() {
    invalidationListeningExecutor.scheduleAtFixedRate(() -> {
      if (trackingConnection.isBroken() || !trackingConnection.isConnected() || !trackingConnection.ping()) {
        // flush cache(broadcasting mode only trackingConnection disconnect)
        clientSideCache.flush();
        // create a new connection and enable tracking
        try {
          trackingConnection = new CacheConnection(jedisSocketFactory, clientConfig, clientSideCache);
        } catch (Exception e) {
          // do something
        }
        tracking();
      }
      trackingConnection.readPushesWithCheckingBroken();
      // period?
    }, 2, 2, TimeUnit.SECONDS);
  }

  private Supplier<Connection> connectionSupplier() {
    return clientSideCache == null ? () -> new Connection(jedisSocketFactory, clientConfig)
        : () -> new CacheConnection(jedisSocketFactory, clientConfig, clientSideCache);
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
}
