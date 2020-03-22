package redis.clients.jedis.commands;

import redis.clients.jedis.DebugParams;

public interface BasicCommands {

  /**
   * This command is often used to test if a connection is still alive, or to measure latency.
   *
   * @return PONG
   */
  String ping();

  /**
   * Ask the server to close the connection. The connection is closed as soon as all pending replies have been written to the client.
   * @return OK
   */
  String quit();

  /**
   * Delete all the keys of the currently selected DB. This command never fails.
   The time-complexity for this operation is O(N), N being the number of keys in the database.
   * @return OK
   */
  String flushDB();

  /**
   * Return the number of keys in the currently-selected database.
   * @return the number of key in the currently-selected database.
   */
  Long dbSize();

  /**
   * Select the DB with having the specified zero-based numeric index.
   * @param index the index
   * @return a simple string reply OK
   */
  String select(int index);

  /**
   * This command swaps two Redis databases, so that immediately all the clients connected to a
   * given database will see the data of the other database, and the other way around.
   * @param index1
   * @param index2
   * @return Simple string reply: OK if SWAPDB was executed correctly.
   */
  String swapDB(int index1, int index2);

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one.
   * @return a simple string reply (OK)
   */
  String flushAll();

  /**
   * Request for authentication in a password-protected Redis server. Redis can be instructed to require a password before allowing clients to execute commands. This is done using the requirepass directive in the configuration file.
   If password matches the password in the configuration file, the server replies with the OK status code and starts accepting commands. Otherwise, an error is returned and the clients needs to try a new password.
   * @param password
   * @return the result of the auth
   */
  String auth(String password);

  /**
   * Request for authentication with username and password, based on the  ACL feature introduced in Redis 6.0
   *   see https://redis.io/topics/acl
   * @param user
   * @param password
   * @return
   */
  String auth(String user, String password);

  /**
   * The SAVE commands performs a synchronous save of the dataset producing a point in time snapshot of all the data inside the Redis instance, in the form of an RDB file.
   You almost never want to call SAVE in production environments where it will block all the other clients. Instead usually BGSAVE is used. However in case of issues preventing Redis to create the background saving child (for instance errors in the fork(2) system call), the SAVE command can be a good last resort to perform the dump of the latest dataset.
   * @return result of the save
   */
  String save();

  /**
   * Save the DB in background. The OK code is immediately returned. Redis forks, the parent continues to serve the clients, the child saves the DB on disk then exits. A client may be able to check if the operation succeeded using the LASTSAVE command.
   * @return ok
   */
  String bgsave();

  /**
   * Instruct Redis to start an Append Only File rewrite process. The rewrite will create a small optimized version of the current Append Only File
   * If BGREWRITEAOF fails, no data gets lost as the old AOF will be untouched.
   The rewrite will be only triggered by Redis if there is not already a background process doing persistence. Specifically:
   If a Redis child is creating a snapshot on disk, the AOF rewrite is scheduled but not started until the saving child producing the RDB file terminates. In this case the BGREWRITEAOF will still return an OK code, but with an appropriate message. You can check if an AOF rewrite is scheduled looking at the INFO command as of Redis 2.6.
   If an AOF rewrite is already in progress the command returns an error and no AOF rewrite will be scheduled for a later time.
   Since Redis 2.4 the AOF rewrite is automatically triggered by Redis, however the BGREWRITEAOF command can be used to trigger a rewrite at any time.
   * @return the response of the command
   */
  String bgrewriteaof();

  /**
   * Return the UNIX TIME of the last DB save executed with success.
   * @return the unix latest save
   */
  Long lastsave();

  /**
   * Stop all the client. Perform a SAVE (if one save point is configured).
   * Flush the append only file if AOF is enabled
   * quit the server
   * @return only in case of error.
   */
  String shutdown();

  /**
   * The INFO command returns information and statistics about the server in a format that is simple to parse by computers and easy to read by humans.
   * @return information on the server
   */
  String info();

  /**
   * The INFO command returns information and statistics about the server in a format that is simple to parse by computers and easy to read by humans.
   * @param section (all: Return all sections, default: Return only the default set of sections, server: General information about the Redis server, clients: Client connections section, memory: Memory consumption related information, persistence: RDB and AOF related information, stats: General statistics, replication: Master/slave replication information, cpu: CPU consumption statistics, commandstats: Redis command statistics, cluster: Redis Cluster section, keyspace: Database related statistics)
   * @return
   */
  String info(String section);

  /**
   * The SLAVEOF command can change the replication settings of a slave on the fly. In the proper form SLAVEOF hostname port will make the server a slave of another server listening at the specified hostname and port.
   * If a server is already a slave of some master, SLAVEOF hostname port will stop the replication against the old server and start the synchronization against the new one, discarding the old dataset.
   * @param host listening at the specified hostname
   * @param port server listening at the specified port
   * @return result of the command.
   */
  String slaveof(String host, int port);

  /**
   *  SLAVEOF NO ONE will stop replication, turning the server into a MASTER, but will not discard the replication. So, if the old master stops working, it is possible to turn the slave into a master and set the application to use this new master in read/write. Later when the other Redis server is fixed, it can be reconfigured to work as a slave.
   * @return result of the command
   */
  String slaveofNoOne();

  /**
   * Return the index of the current database
   * @return the int of the index database.
   */
  int getDB();

  String debug(DebugParams params);

  String configResetStat();

  String configRewrite();

  /**
   * Blocks until all the previous write commands are successfully transferred and acknowledged by 
   * at least the specified number of replicas. 
   * If the timeout, specified in milliseconds, is reached, the command returns 
   * even if the specified number of replicas were not yet reached.
   * 
   * @param replicas successfully transferred and acknowledged by at least the specified number of replicas
   * @param timeout the time to block in milliseconds, a timeout of 0 means to block forever
   * @return the number of replicas reached by all the writes performed in the context of the current connection
   */
  Long waitReplicas(int replicas, long timeout);
}
