package redis.server.stub.command;

import redis.server.stub.ClientState;
import redis.server.stub.RedisDataStore;
import redis.server.stub.RedisServerStub;

/**
 * Context provided to commands during execution.
 *
 * Gives commands access to:
 * - Client state (database, name, authentication, etc.)
 * - Data store (for read/write operations)
 * - Server instance (for configuration, etc.)
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
}

