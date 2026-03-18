package redis.clients.jedis.commands;

/**
 * Commands for managing Redis persistence (RDB snapshots and AOF).
 * <p>
 * These commands operate on a specific Redis node and are only available for single-node
 * connections (e.g., {@code Jedis}). They are NOT suitable for pooled or cluster connections
 * because:
 * <ul>
 * <li>They operate on node-local storage and configuration</li>
 * <li>Commands like SAVE block the entire node</li>
 * <li>Background save operations are node-specific</li>
 * </ul>
 * <p>
 * For cluster deployments, persistence operations should be performed by connecting directly to
 * individual nodes.
 * @see ServerCommands for the full set of server commands
 */
public interface PersistenceCommands {

  /**
   * The SAVE commands performs a synchronous save of the dataset producing a point in time snapshot
   * of all the data inside the Redis instance, in the form of an RDB file. You almost never want to
   * call SAVE in production environments where it will block all the other clients. Instead usually
   * BGSAVE is used. However, in case of issues preventing Redis to create the background saving
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

  /**
   * Schedule a background save operation. The save will be performed only if there is no BGSAVE
   * already scheduled or an AOF rewrite in progress.
   * @return Status code reply
   */
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
}
