package redis.server.stub;

/**
 * Context provided to CommandInterceptor during command execution. Provides access to client state,
 * data store, and server for test verification.
 */
public interface CommandContext {

  /**
   * Get the client state for the connection executing this command.
   * @return client state (subscriptions, database, etc.)
   */
  ClientState getClient();

  /**
   * Get the data store (for state verification).
   * @return Redis data store
   */
  RedisDataStore getDataStore();

  /**
   * Get the server instance (for push injection, etc.).
   * @return RedisServerStub instance
   */
  RedisServerStub getServer();

  /**
   * Get the subscriber for the current connection. Used by pub/sub commands to send push messages.
   * @return subscriber interface
   */
  Subscriber getSubscriber();

}
