package redis.clients.jedis.mcf;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
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

    private static class FailFastFactory extends ConnectionFactory {
        private volatile boolean failFast = false;

        public FailFastFactory(ConnectionFactory.Builder builder) {
            super(builder);
        }

        @Override
        public PooledObject<Connection> makeObject() throws Exception {
            if (failFast) {
                return new DefaultPooledObject<>(new Connection() {
                    @Override
                    public void connect() throws JedisConnectionException {
                        throw new JedisConnectionException("Fail fast mode on!");
                    }

                });
            }
            return super.makeObject();

        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TrackingConnectionPool.class);

    private volatile boolean forcingDisconnect;
    private InitializationTracker<Connection> tracker;

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
        this(ConnectionFactory.builder().setHostAndPort(hostAndPort).setClientConfig(clientConfig)
            .setTracker(createSimpleTracker()));
    }

    private TrackingConnectionPool(ConnectionFactory.Builder builder) {
        super(new FailFastFactory(builder));
        this.tracker = builder.getTracker();
        this.attachAuthenticationListener(builder.getClientConfig().getAuthXManager());
    }

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> poolConfig) {
        this(ConnectionFactory.builder().setHostAndPort(hostAndPort).setClientConfig(clientConfig)
            .setTracker(createSimpleTracker()), poolConfig);
    }

    private TrackingConnectionPool(ConnectionFactory.Builder builder, GenericObjectPoolConfig<Connection> poolConfig) {
        super(new FailFastFactory(builder), poolConfig);
        this.tracker = builder.getTracker();
        this.attachAuthenticationListener(builder.getClientConfig().getAuthXManager());
    }

    @Override
    public Connection getResource() {
        if (forcingDisconnect) {
            throw new JedisConnectionException("Forced disconnect in progress!");
        }

        Connection conn = super.getResource();
        tracker.add(conn);
        return conn;
    }

    @Override
    public void returnResource(final Connection resource) {
        super.returnResource(resource);
        tracker.remove(resource);
    }

    @Override
    public void returnBrokenResource(final Connection resource) {
        if (forcingDisconnect) {
            super.returnResource(resource);
        } else {
            super.returnBrokenResource(resource);
        }
        tracker.remove(resource);
    }

    public void forceDisconnect() {
        ((FailFastFactory) this.getFactory()).setFailFast(true);
        this.forcingDisconnect = true;
        this.clear();
        for (Connection connection : tracker) {
            try {
                connection.forceDisconnect();
            } catch (Exception e) {
                log.warn("Error while force disconnecting connection: " + connection.toIdentityString());
            }
        }
        this.clear();
        this.forcingDisconnect = false;
        ((FailFastFactory) this.getFactory()).setFailFast(false);
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
}
