package redis.server.stub.command;

import redis.server.stub.ClientState;
import redis.server.stub.RedisDataStore;
import redis.server.stub.RedisServerStub;
import redis.server.stub.Subscriber;

/**
 * Context provided to commands during execution.
 *
 * <p>Gives commands access to:
 * <ul>
 *   <li>Client state (database, name, authentication, etc.)</li>
 *   <li>Data store (for read/write operations)</li>
 *   <li>Server instance (for configuration, pub/sub manager, etc.)</li>
 *   <li>Subscriber interface (for pub/sub push messages)</li>
 * </ul>
 */
public interface CommandContext {

    /**
     * Get the client state for the current connection.
     *
     * @return client state
     */
    ClientState getClient();

    /**
     * Get the data store for read/write operations.
     *
     * @return data store
     */
    RedisDataStore getDataStore();

    /**
     * Get the server instance.
     *
     * @return server instance
     */
    RedisServerStub getServer();

    /**
     * Get the subscriber for the current connection.
     * Used by pub/sub commands to send push messages.
     *
     * @return subscriber interface
     */
    Subscriber getSubscriber();
}

