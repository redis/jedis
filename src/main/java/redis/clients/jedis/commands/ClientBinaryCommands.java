package redis.clients.jedis.commands;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.ClientKillParams;

/**
 * The interface contain all the commands about client.
 * The params is byte[]
 */
public interface ClientBinaryCommands {

  /**
   * Close a given client connection.
   *
   * @param ipPort The ip:port should match a line returned by the CLIENT LIST command (addr field).
   * @return close success return OK
   * @see <a href="https://redis.io/commands/client-kill">client kill</a>
   */
  String clientKill(byte[] ipPort);

  /**
   * Close a given client connection.
   *
   * @param ip   The ip should match a line returned by the CLIENT LIST command (addr field).
   * @param port The port should match a line returned by the CLIENT LIST command (addr field).
   * @return close success return OK
   * @see <a href="https://redis.io/commands/client-kill">client kill</a>
   */
  String clientKill(String ip, int port);

  /**
   * Close a given client connection.
   *
   * @param params connect info will be closed
   * @return close success return OK
   * @see <a href="https://redis.io/commands/client-kill">client kill</a>
   */
  long clientKill(ClientKillParams params);

  /**
   * returns the name of the current connection as set by CLIENT SETNAME
   *
   * @return current connect name
   * @see <a href="https://redis.io/commands/client-getname">CLIENT GETNAME</a>
   */
  byte[] clientGetnameBinary();

  /**
   * returns information and statistics about the client connections server
   * in a mostly human readable format.
   *
   * @return all clients info connected to redis-server
   * @see <a href="https://redis.io/commands/client-list">CLIENT LIST</a>
   */
  byte[] clientListBinary();

  /**
   * returns information and statistics about the client connections server
   * in a mostly human readable format filter by client type.
   *
   * @return all clients info connected to redis-server
   * @see <a href="https://redis.io/commands/client-list">CLIENT LIST [TYPE NORMAL|MASTER|REPLICA|PUBSUB] </a>
   */
  byte[] clientListBinary(ClientType type);

  /**
   * returns information and statistics about the client connections server
   * in a mostly human readable format filter by client ids.
   *
   * @param clientIds unique 64-bit client IDs
   * @return all clients info connected to redis-server
   * @see <a href="https://redis.io/commands/client-list">CLIENT LIST [ID client-id [client-id ...]]</a>
   */
  byte[] clientListBinary(long... clientIds);

  /**
   * returns information and statistics about the current client connection
   * in a mostly human readable format.
   *
   * @return information and statistics about the current client connection
   * @see <a href="https://redis.io/commands/client-info">CLIENT INFO</a>
   * @since redis 6.2
   */
  byte[] clientInfoBinary();

  /**
   * assigns a name to the current connection.
   *
   * @param name current connection name
   * @return OK if the connection name was successfully set.
   * @see <a href="https://redis.io/commands/client-setname">CLIENT SETNAME </a>
   */
  String clientSetname(byte[] name);

  /**
   * returns the ID of the current connection.
   *
   * @return The id of the client.
   * @see <a href="https://redis.io/commands/client-id">CLIENT ID</a>
   * @since redis 5.0.0
   */
  long clientId();

  /**
   * Unblock from a different connection, a client blocked in a
   * blocking operation, such as for instance BRPOP or XREAD or WAIT.
   *
   * @param clientId The id of the client
   * @return specifically:
   * 1 if the client was unblocked successfully.
   * 0 if the client wasn't unblocked.
   * @see <a href="https://redis.io/commands/client-unblock">CLIENT UNBLOCK client-id</a>
   * @since redis 5.0.0
   */
  long clientUnblock(long clientId);

  /**
   * Unblock from a different connection, a client blocked in a
   * blocking operation, such as for instance BRPOP or XREAD or WAIT.
   *
   * @param clientId    The id of the client
   * @param unblockType TIMEOUT|ERROR
   * @return specifically:
   * 1 if the client was unblocked successfully.
   * 0 if the client wasn't unblocked.
   * @see <a href="https://redis.io/commands/client-unblock">CLIENT UNBLOCK client-id [TIMEOUT|ERROR]</a>
   * @since redis 5.0.0
   */
  long clientUnblock(long clientId, UnblockType unblockType);

  /**
   * A connections control command able to suspend all the
   * Redis clients for the specified amount of time (in milliseconds)
   *
   * @param timeout WRITE|ALL
   * @return The command returns OK or an error if the timeout is invalid.
   * @see <a href="https://redis.io/commands/client-pause">CLIENT PAUSE timeout</a>
   */
  String clientPause(long timeout);

  /**
   * A connections control command able to suspend all the
   * Redis clients for the specified amount of time (in milliseconds)
   *
   * @param timeout command timeout
   * @param mode    WRITE|ALL
   * @return The command returns OK or an error if the timeout is invalid.
   * @see <a href="https://redis.io/commands/client-pause">CLIENT PAUSE timeout [WRITE|ALL]</a>
   */
  String clientPause(long timeout, ClientPauseMode mode);

}
