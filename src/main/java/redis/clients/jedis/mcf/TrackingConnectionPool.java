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
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.csc.CacheConnection;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class TrackingConnectionPool extends ConnectionPool {

    private static class FailFastConnectionFactory extends ConnectionFactory {
        private boolean failFast = false;
        private final Set<Connection> factoryTrackedObjects = ConcurrentHashMap.newKeySet();

        public FailFastConnectionFactory(ConnectionFactory.Builder factoryBuilder) {
            super(factoryBuilder.setConnectionBuilder(
                customConnectionBuilder(factoryBuilder).setClientConfig(factoryBuilder.getClientConfig())));
        }

        private static Connection.Builder customConnectionBuilder(ConnectionFactory.Builder factoryBuilder) {
            Connection.Builder connBuilder = factoryBuilder.getCache() == null ? Connection.builder()
                : CacheConnection.builder(factoryBuilder.getCache());

            connBuilder.setSocketFactory(factoryBuilder.getJedisSocketFactory())
                .setClientConfig(factoryBuilder.getClientConfig());
            return connBuilder;
        }

        @Override
        public PooledObject<Connection> makeObject() throws Exception {
            if (failFast) {
                throw new JedisConnectionException("Failed to create connection!");
            }
            try {
                PooledObject<Connection> object = super.makeObject();
                factoryTrackedObjects.add(object.getObject());
                try {
                    object.getObject().initializeFromClientConfig();
                } finally {
                    factoryTrackedObjects.remove(object.getObject());
                }
                return object;

            } catch (Exception e) {
                throw new JedisConnectionException("Failed to create connection!", e);
            }
        }

        public void forceDisconnect() {
            for (Connection connection : factoryTrackedObjects) {
                try {
                    connection.forceDisconnect();
                } catch (Exception e) {
                    log.warn("Error while force disconnecting connection: " + connection.toIdentityString());
                }
            }
        }

    }

    public static class Builder {
        private HostAndPort hostAndPort;
        private JedisClientConfig clientConfig;
        private GenericObjectPoolConfig<Connection> poolConfig;

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

        public TrackingConnectionPool build() {
            return new TrackingConnectionPool(this);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TrackingConnectionPool.class);

    private final HostAndPort hostAndPort;
    private final JedisClientConfig clientConfig;
    private final GenericObjectPoolConfig poolConfig;
    private final AtomicInteger numWaiters = new AtomicInteger();
    private final Set<Connection> poolTrackedObjects = ConcurrentHashMap.newKeySet();

    public static Builder builder() {
        return new Builder();
    }

    private TrackingConnectionPool(Builder builder) {
        super(createfailFastFactory(builder),
            builder.poolConfig != null ? builder.poolConfig : new GenericObjectPoolConfig<>());

        this.hostAndPort = builder.hostAndPort;
        this.clientConfig = builder.clientConfig;
        this.poolConfig = builder.poolConfig;
        this.attachAuthenticationListener(builder.clientConfig.getAuthXManager());
    }

    private static FailFastConnectionFactory createfailFastFactory(Builder builder) {
        return new FailFastConnectionFactory(ConnectionFactory.builder().setClientConfig(builder.clientConfig)
            .setJedisSocketFactory(new DefaultJedisSocketFactory(builder.hostAndPort, builder.clientConfig)));
    }

    public static TrackingConnectionPool from(TrackingConnectionPool existing) {
        return builder().hostAndPort(existing.hostAndPort).clientConfig(existing.clientConfig)
            .poolConfig(existing.poolConfig).build();
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
        while (numWaiters.get() > 0 || getNumWaiters() > 0 || getNumActive() > 0 || getNumIdle() > 0) {
            this.clear();
            ((FailFastConnectionFactory) this.getFactory()).forceDisconnect();
            for (Connection connection : poolTrackedObjects) {
                try {
                    connection.forceDisconnect();
                } catch (Exception e) {
                    log.warn("Error while force disconnecting connection: " + connection.toIdentityString());
                }
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
