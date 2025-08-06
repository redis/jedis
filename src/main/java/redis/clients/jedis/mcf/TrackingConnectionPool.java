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

    public static class Builder {
        private HostAndPort hostAndPort;
        private JedisClientConfig clientConfig;
        private GenericObjectPoolConfig<Connection> poolConfig;
        private InitializationTracker<Connection> tracker;

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

        public Builder tracker(InitializationTracker<Connection> tracker) {
            this.tracker = tracker;
            return this;
        }

        public TrackingConnectionPool build() {
            return new TrackingConnectionPool(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> poolConfig) {
        this(builder().hostAndPort(hostAndPort).clientConfig(clientConfig).poolConfig(poolConfig)
            .tracker(createSimpleTracker()));
    }

    private TrackingConnectionPool(Builder builder) {
        super(
            ConnectionFactory.builder().setHostAndPort(builder.hostAndPort).setClientConfig(builder.clientConfig)
                .setTracker(builder.tracker).build(),
            builder.poolConfig != null ? builder.poolConfig : new GenericObjectPoolConfig<>());

        this.tracker = builder.tracker;
        this.clientConfig = builder.clientConfig;
        this.poolConfig = builder.poolConfig;
        this.attachAuthenticationListener(builder.clientConfig.getAuthXManager());
    }

    public static TrackingConnectionPool from(TrackingConnectionPool pool) {
        return builder().clientConfig(pool.clientConfig).poolConfig(pool.poolConfig).tracker(pool.tracker).build();
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
