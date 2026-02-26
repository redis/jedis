package redis.clients.jedis.commands;

import redis.clients.jedis.util.KeyValue;

/**
 * Commands for managing Redis replication.
 * <p>
 * These commands operate on a specific Redis node and are only available for single-node
 * connections (e.g., {@code Jedis}). They are NOT suitable for pooled or cluster connections
 * because:
 * <ul>
 *   <li>They configure node-specific replication topology</li>
 *   <li>SLAVEOF/REPLICAOF changes the node's role in the replication hierarchy</li>
 *   <li>WAIT commands block until replicas acknowledge writes from this specific node</li>
 * </ul>
 * <p>
 * For cluster deployments, replication is managed automatically by the cluster, and these
 * commands should only be used by connecting directly to individual nodes for maintenance.
 * 
 * @see ServerCommands for the full set of server commands
 */
public interface ReplicationCommands {

  /**
   * The SLAVEOF command can change the replication settings of a slave on the fly. In the proper
   * form SLAVEOF hostname port will make the server a slave of another server listening at the
   * specified hostname and port. If a server is already a slave of some master, SLAVEOF hostname
   * port will stop the replication against the old server and start the synchronization against the
   * new one, discarding the old dataset.
   * @param host listening at the specified hostname
   * @param port server listening at the specified port
   * @return result of the command.
   * @deprecated Use {@link ReplicationCommands#replicaof(java.lang.String, int)}.
   */
  @Deprecated
  String slaveof(String host, int port);

  /**
   * SLAVEOF NO ONE will stop replication, turning the server into a MASTER, but will not discard
   * the replication. So, if the old master stops working, it is possible to turn the slave into a
   * master and set the application to use this new master in read/write. Later when the other Redis
   * server is fixed, it can be reconfigured to work as a slave.
   * @return result of the command
   * @deprecated Use {@link ReplicationCommands#replicaofNoOne()}.
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
   * Synchronous replication of Redis as described here: http://antirez.com/news/66.
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

  /**
   * Blocks the current client until all the previous write commands are acknowledged as having been
   * fsynced to the AOF of the local Redis and/or at least the specified number of replicas.
   * <a href="https://redis.io/commands/waitaof/">Redis Documentation</a>
   * @param numLocal Number of local instances that are required to acknowledge the sync (0 or 1),
   *                 cannot be non-zero if the local Redis does not have AOF enabled
   * @param numReplicas Number of replicas that are required to acknowledge the sync
   * @param timeout Timeout in millis of the operation - if 0 timeout is unlimited. If the timeout is reached,
   *                the command returns even if the specified number of acknowledgments has not been met.
   * @return KeyValue where Key is number of local Redises (0 or 1) that have fsynced to AOF all writes
   * performed in the context of the current connection, and the value is the number of replicas that have acknowledged doing the same.
   */
  KeyValue<Long, Long> waitAOF(long numLocal, long numReplicas, long timeout);
}

