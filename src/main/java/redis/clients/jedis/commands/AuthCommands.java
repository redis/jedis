package redis.clients.jedis.commands;

/**
 * Commands for Redis authentication.
 * <p>
 * These commands operate on the current connection and are primarily used for single-node
 * connections (e.g., {@code Jedis}). For pooled or cluster connections, authentication is typically
 * configured at the connection/pool level rather than per-command.
 * <p>
 * Note: In most cases, authentication credentials should be provided when creating the connection
 * or pool, rather than calling these commands directly.
 * @see ServerCommands for the full set of server commands
 */
public interface AuthCommands {

  /**
   * Request for authentication in a password-protected Redis server. Redis can be instructed to
   * require a password before allowing clients to execute commands. This is done using the
   * requirepass directive in the configuration file. If password matches the password in the
   * configuration file, the server replies with the OK status code and starts accepting commands.
   * Otherwise, an error is returned and the clients needs to try a new password.
   * @return the result of the auth
   */
  String auth(String password);

  /**
   * Request for authentication with username and password, based on the ACL feature introduced in
   * Redis 6.0 see https://redis.io/topics/acl
   * @return OK
   */
  String auth(String user, String password);
}
