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
   */
  String clientKill(byte[] ipPort);

  /**
   * Close a given client connection.
   *
   * @param ip   The ip should match a line returned by the CLIENT LIST command (addr field).
   * @param port The port should match a line returned by the CLIENT LIST command (addr field).
   * @return Close success return OK
   */
  String clientKill(String ip, int port);

  /**
   * Close a given client connection.
   *
   * @param params connect info will be closed
   * @return Close success return OK
   */
  long clientKill(ClientKillParams params);

  /**
   * Returns the name of the current connection as set by CLIENT SETNAME
   *
   * @return Current connect name
   */
  byte[] clientGetnameBinary();

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human readable format.
   *
   * @return All clients info connected to redis-server
   */
  byte[] clientListBinary();

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human readable format filter by client type.
   *
   * @return all clients info connected to redis-server
   */
  byte[] clientListBinary(ClientType type);

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human readable format filter by client ids.
   *
   * @param clientIds Unique 64-bit client IDs
   * @return All clients info connected to redis-server
   */
  byte[] clientListBinary(long... clientIds);

  /**
   * Returns information and statistics about the current client connection
   * in a mostly human readable format.
   *
   * @return Information and statistics about the current client connection
   */
  byte[] clientInfoBinary();

  /**
   * Assigns a name to the current connection.
   *
   * @param name Current connection name
   * @return OK if the connection name was successfully set.
   */
  String clientSetname(byte[] name);

  /**
   * Returns the ID of the current connection.
   *
   * @return The id of the client.
   */
  long clientId();

  /**
   * Unblock from a different connection, a client blocked in a
   * blocking operation, such as for instance BRPOP or XREAD or WAIT.
   *
   * @param clientId The id of the client
   * @return Specifically:
   * 1 if the client was unblocked successfully.
   * 0 if the client wasn't unblocked.
   */
  long clientUnblock(long clientId);

  /**
   * Unblock from a different connection, a client blocked in a
   * blocking operation, such as for instance BRPOP or XREAD or WAIT.
   *
   * @param clientId    The id of the client
   * @param unblockType TIMEOUT|ERROR
   * @return Specifically:
   * 1 if the client was unblocked successfully.
   * 0 if the client wasn't unblocked.
   */
  long clientUnblock(long clientId, UnblockType unblockType);

  /**
   * A connections control command able to suspend all the
   * Redis clients for the specified amount of time (in milliseconds)
   *
   * @param timeout WRITE|ALL
   * @return The command returns OK or an error if the timeout is invalid.
   */
  String clientPause(long timeout);

  /**
   * A connections control command able to suspend all the
   * Redis clients for the specified amount of time (in milliseconds)
   *
   * @param timeout Command timeout
   * @param mode    WRITE|ALL
   * @return The command returns OK or an error if the timeout is invalid.
   */
  String clientPause(long timeout, ClientPauseMode mode);

  /**
   * Turn on the client eviction mode for the current connection.
   *
   * @return OK
   */
  String clientNoEvictOn();

  /**
   * Turn off the client eviction mode for the current connection.
   *
   * @return OK
   */
  String clientNoEvictOff();
}
