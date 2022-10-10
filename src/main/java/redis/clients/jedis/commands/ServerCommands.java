package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.SaveMode;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.LolwutParams;
import redis.clients.jedis.params.ShutdownParams;

public interface ServerCommands {

  /**
   * This command is often used to test if a connection is still alive, or to measure latency.
   * @return PONG
   */
  String ping();

  String ping(String message);

  String echo(String string);

  byte[] echo(byte[] arg);

  /**
   * Ask the server to close the connection. The connection is closed as soon as all pending replies
   * have been written to the client.
   * @return OK
   */
  String quit();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * @return OK
   */
  String flushDB();

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one.
   * @return a simple string reply (OK)
   */
  String flushAll();

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one.
   * @param flushMode
   * @return a simple string reply (OK)
   */
  String flushAll(FlushMode flushMode);

  /**
   * Request for authentication in a password-protected Redis server. Redis can be instructed to
   * require a password before allowing clients to execute commands. This is done using the
   * requirepass directive in the configuration file. If password matches the password in the
   * configuration file, the server replies with the OK status code and starts accepting commands.
   * Otherwise, an error is returned and the clients needs to try a new password.
   * @param password
   * @return the result of the auth
   */
  String auth(String password);

  /**
   * Request for authentication with username and password, based on the ACL feature introduced in
   * Redis 6.0 see https://redis.io/topics/acl
   * @param user
   * @param password
   * @return OK
   */
  String auth(String user, String password);

  /**
   * The SAVE commands performs a synchronous save of the dataset producing a point in time snapshot
   * of all the data inside the Redis instance, in the form of an RDB file. You almost never want to
   * call SAVE in production environments where it will block all the other clients. Instead usually
   * BGSAVE is used. However in case of issues preventing Redis to create the background saving
   * child (for instance errors in the fork(2) system call), the SAVE command can be a good last
   * resort to perform the dump of the latest dataset.
   * @return result of the save
   */
  String save();

  /**
   * Save the DB in background. The OK code is immediately returned. Redis forks, the parent
   * continues to serve the clients, the child saves the DB on disk then exits. A client may be able
   * to check if the operation succeeded using the LASTSAVE command.
   * @return ok
   */
  String bgsave();

  String bgsaveSchedule();

  /**
   * Instruct Redis to start an Append Only File rewrite process. The rewrite will create a small
   * optimized version of the current Append Only File If BGREWRITEAOF fails, no data gets lost as
   * the old AOF will be untouched. The rewrite will be only triggered by Redis if there is not
   * already a background process doing persistence. Specifically: If a Redis child is creating a
   * snapshot on disk, the AOF rewrite is scheduled but not started until the saving child producing
   * the RDB file terminates. In this case the BGREWRITEAOF will still return an OK code, but with
   * an appropriate message. You can check if an AOF rewrite is scheduled looking at the INFO
   * command as of Redis 2.6. If an AOF rewrite is already in progress the command returns an error
   * and no AOF rewrite will be scheduled for a later time. Since Redis 2.4 the AOF rewrite is
   * automatically triggered by Redis, however the BGREWRITEAOF command can be used to trigger a
   * rewrite at any time.
   * @return the response of the command
   */
  String bgrewriteaof();

  /**
   * Return the UNIX TIME of the last DB save executed with success.
   * @return the unix latest save
   */
  long lastsave();

  /**
   * Stop all the client. Perform a SAVE (if one save point is configured). Flush the append only
   * file if AOF is enabled quit the server
   * @throws JedisException only in case of error.
   */
  void shutdown() throws JedisException;

  /**
   * @see SaveMode
   * @param saveMode modifier to alter the data save behavior of SHUTDOWN. {@code null} would
   * trigger the default behavior.
   * @throws JedisException
   * @deprecated Use {@link ServerCommands#shutdown(redis.clients.jedis.params.ShutdownParams)}.
   */
  @Deprecated
  void shutdown(SaveMode saveMode) throws JedisException;

  /**
   * @see SaveMode
   * @param shutdownParams set commands parameters
   * @throws JedisException
   */
  void shutdown(ShutdownParams shutdownParams) throws JedisException;

  String shutdownAbort();

  /**
   * The INFO command returns information and statistics about the server in a format that is simple
   * to parse by computers and easy to read by humans.
   * @return information on the server
   */
  String info();

  /**
   * The INFO command returns information and statistics about the server in a format that is simple
   * to parse by computers and easy to read by humans.
   * @param section (all: Return all sections, default: Return only the default set of sections,
   *          server: General information about the Redis server, clients: Client connections
   *          section, memory: Memory consumption related information, persistence: RDB and AOF
   *          related information, stats: General statistics, replication: Master/slave replication
   *          information, cpu: CPU consumption statistics, commandstats: Redis command statistics,
   *          cluster: Redis Cluster section, keyspace: Database related statistics)
   * @return info
   */
  String info(String section);

  /**
   * The SLAVEOF command can change the replication settings of a slave on the fly. In the proper
   * form SLAVEOF hostname port will make the server a slave of another server listening at the
   * specified hostname and port. If a server is already a slave of some master, SLAVEOF hostname
   * port will stop the replication against the old server and start the synchronization against the
   * new one, discarding the old dataset.
   * @param host listening at the specified hostname
   * @param port server listening at the specified port
   * @return result of the command.
   * @deprecated Use {@link ServerCommands#replicaof(java.lang.String, int)}.
   */
  @Deprecated
  String slaveof(String host, int port);

  /**
   * SLAVEOF NO ONE will stop replication, turning the server into a MASTER, but will not discard
   * the replication. So, if the old master stops working, it is possible to turn the slave into a
   * master and set the application to use this new master in read/write. Later when the other Redis
   * server is fixed, it can be reconfigured to work as a slave.
   * @return result of the command
   * @deprecated Use {@link ServerCommands#replicaofNoOne()}.
   */
  @Deprecated
  String slaveofNoOne();

  /**
   * The REPLICAOF command can change the replication settings of a replica on the fly. In the
   * proper form REPLICAOF hostname port will make the server a replica of another server
   * listening at the specified hostname and port. If a server is already a replica of some master,
   * REPLICAOF hostname port will stop the replication against the old server and start the
   * synchronization against the new one, discarding the old dataset.
   * @param host listening at the specified hostname
   * @param port server listening at the specified port
   * @return result of the command.
   */
  String replicaof(String host, int port);

  /**
   * REPLICAOF NO ONE will stop replication, turning the server into a MASTER, but will not discard
   * the replication. So, if the old master stops working, it is possible to turn the replica into
   * a master and set the application to use this new master in read/write. Later when the other
   * Redis server is fixed, it can be reconfigured to work as a replica.
   * @return result of the command
   */
  String replicaofNoOne();

  /**
   * Syncrhonous replication of Redis as described here: http://antirez.com/news/66.
   * <p>
   * Blocks until all the previous write commands are successfully transferred and acknowledged by
   * at least the specified number of replicas. If the timeout, specified in milliseconds, is
   * reached, the command returns even if the specified number of replicas were not yet reached.
   * <p>
   * Since Java Object class has implemented {@code wait} method, we cannot use it.
   * @param replicas successfully transferred and acknowledged by at least the specified number of
   *          replicas
   * @param timeout the time to block in milliseconds, a timeout of 0 means to block forever
   * @return the number of replicas reached by all the writes performed in the context of the
   *         current connection
   */
  long waitReplicas(int replicas, long timeout);

  String lolwut();

  String lolwut(LolwutParams lolwutParams);
}
