package redis.clients.jedis.mcf;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.InitializationTracker;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class TrackingConnectionPool extends ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(TrackingConnectionPool.class);

    private final InitializationTracker<Connection> tracker;
    private final GenericObjectPoolConfig poolConfig;
    private final JedisClientConfig clientConfig;
    private final AtomicInteger numWaiters = new AtomicInteger();

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> poolConfig) {
        this(ConnectionFactory.builder().setHostAndPort(hostAndPort).setClientConfig(clientConfig)
            .setTracker(createSimpleTracker()), poolConfig);
    }

    private TrackingConnectionPool(ConnectionFactory.Builder builder, GenericObjectPoolConfig<Connection> poolConfig) {
        super(new ConnectionFactory(builder), poolConfig);
        this.tracker = builder.getTracker();
        this.clientConfig = builder.getClientConfig();
        this.poolConfig = poolConfig;
        this.attachAuthenticationListener(builder.getClientConfig().getAuthXManager());
    }

    public static TrackingConnectionPool from(TrackingConnectionPool pool) {
        return new TrackingConnectionPool(pool);
    }

    private TrackingConnectionPool(TrackingConnectionPool pool) {
        super(pool.getFactory());
        this.tracker = pool.tracker;
        this.clientConfig = pool.clientConfig;
        this.attachAuthenticationListener(clientConfig.getAuthXManager());

        this.poolConfig = pool.poolConfig;
        if (pool.poolConfig != null) {
            this.setConfig(pool.poolConfig);
        }
    }

    @Override
    public Connection getResource() {
        try {
            numWaiters.incrementAndGet();
            Connection conn = super.getResource();
            tracker.add(conn);
            return conn;
        } catch (Exception e) {
            if (this.isClosed()) {
                throw new JedisConnectionException("Pool is closed", e);
            }
            throw e;
        } finally {
            numWaiters.decrementAndGet();
        }
    }

    @Override
    public void returnResource(final Connection resource) {
        super.returnResource(resource);
        tracker.remove(resource);
    }

    @Override
    public void returnBrokenResource(final Connection resource) {
        super.returnBrokenResource(resource);
        tracker.remove(resource);
    }

    public void forceDisconnect() {
        this.close();
        while (numWaiters.get() > 0 || getNumWaiters() > 0 || getNumActive() > 0 || getNumIdle() > 0) {
            this.clear();
            for (Connection connection : tracker) {
                try {
                    connection.forceDisconnect();
                } catch (Exception e) {
                    log.warn("Error while force disconnecting connection: " + connection.toIdentityString());
                }
            }
        }
    }

    private static InitializationTracker<Connection> createSimpleTracker() {
        return new InitializationTracker<Connection>() {
            private final Set<Connection> allCreatedObjects = ConcurrentHashMap.newKeySet();

            @Override
            public void add(Connection target) {
                allCreatedObjects.add(target);
            }

            @Override
            public void remove(Connection target) {
                allCreatedObjects.remove(target);
            }

            @Override
            public Iterator<Connection> iterator() {
                return allCreatedObjects.iterator();
            }
        };
    }

    @Override
    public void close() {
        this.destroy();
        this.detachAuthenticationListener();
    }
}
