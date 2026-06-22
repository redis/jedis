package redis.clients.jedis.mcf;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class TrackingConnectionPool extends ConnectionPool {

  private static class FailFastConnectionFactory extends ConnectionFactory {
    private volatile boolean failFast = false;
    private final Set<Connection> factoryTrackedObjects = ConcurrentHashMap.newKeySet();

    private static class FailFastFactoryBuilder extends ConnectionFactory.Builder {

      @Override
      protected ConnectionFactory create() {
        return new FailFastConnectionFactory(this);
      }
    }

    public FailFastConnectionFactory(Builder factoryBuilder) {
      super(factoryBuilder);
    }

    @Override
    public PooledObject<Connection> makeObject() throws Exception {
      if (failFast) {
        throw new JedisConnectionException("Failed to create connection!");
      }
      try {
        PooledObject<Connection> object = super.makeObject();
        // this can make a marginal improvement on fast failover duration!
        if (failFast) {
          object.getObject().close();
          throw new JedisConnectionException("Failed to create connection!");
        }
        return object;
      } catch (JedisConnectionException e) {
        throw e;
      } catch (Exception e) {
        throw new JedisConnectionException(e);
      }
    }

    @Override
    protected void initialize(Connection conn) {
      // Track the connection while it is being initialized so forceDisconnect() can interrupt
      // a thread that is blocked inside HELLO/AUTH/CLIENT round-trips.
      factoryTrackedObjects.add(conn);
      try {
        super.initialize(conn);
      } finally {
        factoryTrackedObjects.remove(conn);
      }
    }

    public void forceDisconnect() {
      for (Connection connection : factoryTrackedObjects) {
        try {
          connection.forceDisconnect();
        } catch (Exception e) {
          log.warn("Error while force disconnecting connection: " + connection.toIdentityString(),
            e);
        }
      }
    }

  }

  public static class Builder {
    private HostAndPort hostAndPort;
    private JedisClientConfig clientConfig;
    private GenericObjectPoolConfig<Connection> poolConfig;
    private MaintenanceNotificationsConfig maintenanceNotificationsConfig;

    public Builder hostAndPort(HostAndPort hostAndPort) {
      this.hostAndPort = hostAndPort;
      return this;
    }

    public Builder clientConfig(JedisClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }

    public Builder poolConfig(GenericObjectPoolConfig<Connection> poolConfig) {
      this.poolConfig = poolConfig;
      return this;
    }

    public Builder maintenanceNotificationsConfig(
        MaintenanceNotificationsConfig maintenanceNotificationsConfig) {
      this.maintenanceNotificationsConfig = maintenanceNotificationsConfig;
      return this;
    }

    public TrackingConnectionPool build() {
      applyDefaults();
      return new TrackingConnectionPool(this);
    }

    private void applyDefaults() {
      if (clientConfig == null) {
        clientConfig = DefaultJedisClientConfig.builder().build();
      }
      if (poolConfig == null) {
        poolConfig = new GenericObjectPoolConfig<>();
      }
    }
  }

  private static final Logger log = LoggerFactory.getLogger(TrackingConnectionPool.class);

  private final HostAndPort hostAndPort;
  private final JedisClientConfig clientConfig;
  private final GenericObjectPoolConfig<Connection> poolConfig;
  private final MaintenanceNotificationsConfig maintenanceNotificationsConfig;
  private final AtomicInteger numWaiters = new AtomicInteger();
  private final Set<Connection> poolTrackedObjects = ConcurrentHashMap.newKeySet();

  public static Builder builder() {
    return new Builder();
  }

  private TrackingConnectionPool(Builder builder) {
    super(createFailFastFactoryBuilder(builder),
        builder.poolConfig != null ? builder.poolConfig : new GenericObjectPoolConfig<>(),
        builder.maintenanceNotificationsConfig);

    this.hostAndPort = builder.hostAndPort;
    this.clientConfig = builder.clientConfig;
    this.poolConfig = builder.poolConfig;
    this.maintenanceNotificationsConfig = builder.maintenanceNotificationsConfig;
    this.attachAuthenticationListener(builder.clientConfig.getAuthXManager());
  }

  private static ConnectionFactory.Builder createFailFastFactoryBuilder(Builder poolBuilder) {
    return new FailFastConnectionFactory.FailFastFactoryBuilder()
        .hostAndPort(poolBuilder.hostAndPort).clientConfig(poolBuilder.clientConfig);
  }

  public static TrackingConnectionPool from(TrackingConnectionPool existing) {
    return builder().hostAndPort(existing.hostAndPort).clientConfig(existing.clientConfig)
        .poolConfig(existing.poolConfig)
        .maintenanceNotificationsConfig(existing.maintenanceNotificationsConfig).build();
  }

  @Override
  public Connection getResource() {
    try {
      numWaiters.incrementAndGet();
      Connection conn = super.getResource();
      poolTrackedObjects.add(conn);
      return conn;
    } catch (Exception e) {
      if (this.isClosed()) {
        throw new JedisConnectionException("Pool is closed!", e);
      }
      throw e;
    } finally {
      numWaiters.decrementAndGet();
    }
  }

  @Override
  public void returnResource(final Connection resource) {
    super.returnResource(resource);
    poolTrackedObjects.remove(resource);
  }

  @Override
  public void returnBrokenResource(final Connection resource) {
    super.returnBrokenResource(resource);
    poolTrackedObjects.remove(resource);
  }

  public void forceDisconnect() {
    this.close();
    ((FailFastConnectionFactory) this.getFactory()).failFast = true;
    int numOfConnected = poolTrackedObjects.size();
    // we need to wait for all waiters to leave before we are done with disconnecting the
    // connections, since a user app thread might be either;
    // - in the middle of a factory call(create|init) and not yet show up in poolTrackedObjects
    // - blocked on an exhausted pool, waiting for resources to return back pool
    while (numWaiters.get() > 0 || numOfConnected > 0) {
      this.clear();
      ((FailFastConnectionFactory) this.getFactory()).forceDisconnect();
      numOfConnected = 0;
      for (Connection connection : poolTrackedObjects) {
        try {
          if (connection.isConnected()) {
            numOfConnected++;
          }
          connection.forceDisconnect();
        } catch (Exception e) {
          log.warn("Error while force disconnecting connection: " + connection.toIdentityString(),
            e);
        }
      }
      try {
        // this is just to yield the thread for a fair share of CPU
        Thread.sleep(1);
      } catch (InterruptedException e) {
      }
    }
    ((FailFastConnectionFactory) this.getFactory()).failFast = false;
  }

  @Override
  public void close() {
    this.destroy();
    this.detachAuthenticationListener();
  }
}
