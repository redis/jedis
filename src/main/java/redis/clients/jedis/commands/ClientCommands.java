package redis.clients.jedis.commands;

import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.resps.TrackingInfo;

/**
 * The interface contain all the commands about client.
 * The params is String encoded in uft-8
 */
public interface ClientCommands {

  /**
   * Close a given client connection.
   *
   * @param ipPort The ip:port should match a line returned by the CLIENT LIST command (addr field).
   * @return Close success return OK
   */
  String clientKill(String ipPort);

  /**
   * Close a given client connection.
   *
   * @param ip   The ip should match a line returned by the CLIENT LIST command (addr field).
   * @param port The port should match a line returned by the CLIENT LIST command (addr field).
   * @return Close success return OK
   */
  String clientKill(String ip, int port);

  /**
   * Close client connections based on certain selection parameters.
   *
   * @param params Parameters defining what client connections to close.
   * @return The number of client connections that were closed.
   */
  long clientKill(ClientKillParams params);

  /**
   * Returns the name of the current connection as set by CLIENT SETNAME
   *
   * @return Current connect name
   */
  String clientGetname();

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human-readable format.
   *
   * @return All clients info connected to redis-server
   */
  String clientList();

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human-readable format filter by client type.
   *
   * @return All clients info connected to redis-server
   */
  String clientList(ClientType type);

  /**
   * Returns information and statistics about the client connections server
   * in a mostly human-readable format filter by client ids.
   *
   * @param clientIds Unique 64-bit client IDs
   * @return All clients info connected to redis-server
   */
  String clientList(long... clientIds);

  /**
   * Returns information and statistics about the current client connection
   * in a mostly human-readable format.
   *
   * @return Information and statistics about the current client connection
   */
  String clientInfo();

  /**
   * client set info command
   * Since redis 7.2
   * @param attr the attr option
   * @param value the value
   * @return OK or error
   */
  String clientSetInfo(ClientAttributeOption attr, String value);

  /**
   * Assigns a name to the current connection.
   *
   * @param name current connection name
   * @return OK if the connection name was successfully set.
   */
  String clientSetname(String name);

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
   * @return 1 if the client was unblocked successfully, 0 if the client wasn't unblocked.
   */
  long clientUnblock(long clientId);

  /**
   * Unblock from a different connection, a client blocked in a
   * blocking operation, such as for instance BRPOP or XREAD or WAIT.
   *
   * @param clientId    The id of the client
   * @param unblockType TIMEOUT|ERROR
   * @return 1 if the client was unblocked successfully, 0 if the client wasn't unblocked.
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
   * CLIENT UNPAUSE is used to resume command processing for all clients that were paused by CLIENT PAUSE.
   * @return OK
   */
  String clientUnpause();

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

  /**
   * Turn on <a href="https://redis.io/commands/client-no-touch/">CLIENT NO-TOUCH</a>
   * @return OK
   */
  String clientNoTouchOn();

  /**
   * Turn off <a href="https://redis.io/commands/client-no-touch/">CLIENT NO-TOUCH</a>
   * @return OK
   */
  String clientNoTouchOff();

  TrackingInfo clientTrackingInfo();
}
