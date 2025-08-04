package redis.clients.jedis.mcf;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class TrackingConnectionPool extends ConnectionPool {
    private static final Logger log = LoggerFactory.getLogger(TrackingConnectionPool.class);

    private final Set<Connection> allCreatedObjects = ConcurrentHashMap.newKeySet();
    private volatile boolean forcingDisconnect;
    private ConnectionFactory factory;

    // Executor for running connection creation subtasks
    private final ExecutorService connectionCreationExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "connection-creator");
        t.setDaemon(true);
        return t;
    });

    // Simple latch for external unblocking of all connection creation threads
    private volatile CountDownLatch unblockLatch = new CountDownLatch(1);

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig,
        GenericObjectPoolConfig<Connection> poolConfig) {
        super(hostAndPort, clientConfig, poolConfig);
        this.factory = (ConnectionFactory) this.getFactory();
        factory.injectMaker(this::injector);
    }

    private Supplier<Connection> injector(Supplier<Connection> supplier) {
        return () -> make(supplier);
    }

    private Connection make(Supplier<Connection> supplier) {
        if (forcingDisconnect) {
            return new Connection(){
                @Override
                public void connect() {
                    throw new JedisConnectionException("Forced disconnect in progress!");
                }
            };
        }
        // Create CompletableFutures for both the connection task and unblock signal
        CompletableFuture<Connection> connectionFuture = CompletableFuture.supplyAsync(() -> supplier.get(),
            connectionCreationExecutor);

        CompletableFuture<Void> unblockFuture = CompletableFuture.runAsync(() -> {
            try {
                unblockLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, connectionCreationExecutor);

        try {
            // Wait for whichever completes first
            CompletableFuture.anyOf(connectionFuture, unblockFuture).join();

            if (connectionFuture.isDone() && !connectionFuture.isCompletedExceptionally()) {
                return connectionFuture.join();
            } else {
                connectionFuture.cancel(true);
                throw new JedisConnectionException("Connection creation was cancelled due to forced disconnect!");
            }
        } catch (JedisConnectionException e) {
            connectionFuture.cancel(true);
            unblockFuture.cancel(true);
            throw e;
        } catch (Exception e) {
            connectionFuture.cancel(true);
            unblockFuture.cancel(true);
            throw new JedisConnectionException("Connection creation failed", e);
        }
    }

    public TrackingConnectionPool(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
        super(hostAndPort, clientConfig);
        this.factory = (ConnectionFactory) this.getFactory();
    }

    @Override
    public Connection getResource() {
        if (forcingDisconnect) {
            throw new JedisConnectionException("Forced disconnect in progress!");
        }

        Connection conn = super.getResource();
        allCreatedObjects.add(conn);
        return conn;
    }

    @Override
    public void returnResource(final Connection resource) {
        super.returnResource(resource);
        allCreatedObjects.remove(resource);
    }

    @Override
    public void returnBrokenResource(final Connection resource) {
        if (forcingDisconnect) {
            super.returnResource(resource);
        } else {
            super.returnBrokenResource(resource);
        }
        allCreatedObjects.remove(resource);
    }

    public void forceDisconnect() {
        this.forcingDisconnect = true;

        // First, unblock any pending connection creation
        unblockConnectionCreation();

        this.clear();
        for (Connection connection : allCreatedObjects) {
            try {
                connection.forceDisconnect();
            } catch (Exception e) {
                log.warn("Error while force disconnecting connection: " + connection.toIdentityString());
            }
        }
        this.clear();
        this.forcingDisconnect = false;
    }

    /**
     * Unblock ALL waiting connection creation threads.
     */
    private void unblockConnectionCreation() {
        CountDownLatch oldLatch = unblockLatch;
        unblockLatch = new CountDownLatch(1); // Reset first
        oldLatch.countDown(); // Then signal old one
        log.info("Externally unblocked waiting connection creation threads");
    }

    @Override
    public void close() {
        // Shutdown the connection creation executor
        connectionCreationExecutor.shutdown();
        try {
            if (!connectionCreationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                connectionCreationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            connectionCreationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        super.close();
    }
}
