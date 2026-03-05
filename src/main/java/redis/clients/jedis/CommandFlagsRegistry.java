package redis.clients.jedis;

import java.util.EnumSet;

/**
 * Registry interface for command flags. Provides a mapping from Redis commands to their flags. This
 * interface allows for different implementations of the flags registry.
 */
public interface CommandFlagsRegistry {

  /**
   * Command flags based on command flags exposed by Redis. See
   * <a href="https://redis.io/docs/latest/commands/command/#flags">Command flags</a> for more
   * details.
   * <p>
   * Flags description:
   * <ul>
   * <li>READONLY: Command doesn't modify data</li>
   * <li>WRITE: Command may modify data</li>
   * <li>DENYOOM: Command may increase memory usage (deny if out of memory)</li>
   * <li>ADMIN: Administrative command</li>
   * <li>PUBSUB: Pub/Sub related command</li>
   * <li>NOSCRIPT: Command not allowed in scripts</li>
   * <li>SORT_FOR_SCRIPT: Command output needs sorting for scripts</li>
   * <li>LOADING: Command allowed while database is loading</li>
   * <li>STALE: Command allowed on stale replicas</li>
   * <li>SKIP_MONITOR: Command not shown in MONITOR output</li>
   * <li>ASKING: Command allowed in cluster ASKING state</li>
   * <li>FAST: Command has O(1) time complexity</li>
   * <li>MOVABLEKEYS: Command key positions may vary</li>
   * <li>MODULE: Module command</li>
   * <li>BLOCKING: Command may block the client</li>
   * <li>NO_AUTH: Command allowed without authentication</li>
   * <li>NO_ASYNC_LOADING: Command not allowed during async loading</li>
   * <li>NO_MULTI: Command not allowed in MULTI/EXEC</li>
   * <li>NO_MANDATORY_KEYS: Command may work without keys</li>
   * <li>ALLOW_BUSY: Command allowed when server is busy</li>
   * </ul>
   */
  enum CommandFlag {
    READONLY, WRITE, DENYOOM, ADMIN, PUBSUB, NOSCRIPT, SORT_FOR_SCRIPT, LOADING, STALE,
    SKIP_MONITOR, SKIP_SLOWLOG, ASKING, FAST, MOVABLEKEYS, MODULE, BLOCKING, NO_AUTH,
    NO_ASYNC_LOADING, NO_MULTI, NO_MANDATORY_KEYS, ALLOW_BUSY
  }

  /**
   * Request policy for commands in a clustered deployment. This tip helps clients determine which
   * shards to send the command to in clustering mode. See
   * <a href="https://redis.io/docs/latest/develop/reference/command-tips/#request_policy"> Request
   * policy</a> for more details.
   * <p>
   * Policy values:
   * <ul>
   * <li>DEFAULT: No specific request policy defined. For commands without key arguments, execute on
   * an arbitrary shard. For commands with key arguments, route to a single shard based on the hash
   * slot of input keys.</li>
   * <li>ALL_NODES: Execute the command on all nodes - masters and replicas alike. Example: CONFIG
   * SET. Used by commands that don't accept key name arguments and operate atomically per
   * shard.</li>
   * <li>ALL_SHARDS: Execute the command on all master shards. Example: DBSIZE. Used by commands
   * that don't accept key name arguments and operate atomically per shard.</li>
   * <li>MULTI_SHARD: Execute the command on several shards. The client should split the inputs
   * according to the hash slots of input key name arguments. Example: DEL, MSET, MGET.</li>
   * <li>SPECIAL: Indicates a non-trivial form of request policy. Example: SCAN.</li>
   * </ul>
   */
  enum RequestPolicy {
    DEFAULT, ALL_NODES, ALL_SHARDS, MULTI_SHARD, SPECIAL
  }

  /**
   * Response policy for commands in a clustered deployment. This tip helps clients determine how to
   * aggregate replies from multiple shards in a cluster. See
   * <a href="https://redis.io/docs/latest/develop/reference/command-tips/#response_policy">
   * Response policy</a> for more details.
   * <p>
   * Policy values:
   * <ul>
   * <li>DEFAULT: No specific response policy defined. For commands without key arguments, aggregate
   * all replies within a single nested data structure. For commands with key arguments, retain the
   * same order of replies as the input key names.</li>
   * <li>ONE_SUCCEEDED: Return success if at least one shard didn't reply with an error. Reply with
   * the first non-error reply obtained. Example: SCRIPT KILL.</li>
   * <li>ALL_SUCCEEDED: Return successfully only if there are no error replies. A single error reply
   * should disqualify the aggregate. Example: CONFIG SET, SCRIPT FLUSH.</li>
   * <li>AGG_LOGICAL_AND: Return the result of a logical AND operation on all replies. Only applies
   * to integer replies (0 or 1). Example: SCRIPT EXISTS.</li>
   * <li>AGG_LOGICAL_OR: Return the result of a logical OR operation on all replies. Only applies to
   * integer replies (0 or 1).</li>
   * <li>AGG_MIN: Return the minimal value from the replies. Only applies to numerical replies.
   * Example: WAIT.</li>
   * <li>AGG_MAX: Return the maximal value from the replies. Only applies to numerical replies.</li>
   * <li>AGG_SUM: Return the sum of replies. Only applies to numerical replies. Example:
   * DBSIZE.</li>
   * <li>SPECIAL: Indicates a non-trivial form of reply policy. Example: INFO.</li>
   * </ul>
   */
  enum ResponsePolicy {
    DEFAULT, ONE_SUCCEEDED, ALL_SUCCEEDED, AGG_LOGICAL_AND, AGG_LOGICAL_OR, AGG_MIN, AGG_MAX,
    AGG_SUM, SPECIAL
  }

  /**
   * Get the flags for a given command.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return EnumSet of CommandFlag for this command, or empty set if command has no flags
   */
  EnumSet<CommandFlag> getFlags(CommandArguments commandArguments);

  /**
   * Get the request policy for a given command. The request policy helps clients determine which
   * shards to send the command to in a clustered deployment.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return RequestPolicy for this command, or DEFAULT if no specific policy is defined
   */
  RequestPolicy getRequestPolicy(CommandArguments commandArguments);

  /**
   * Get the response policy for a given command. The response policy helps clients determine how to
   * aggregate replies from multiple shards in a cluster.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return ResponsePolicy for this command, or DEFAULT if no specific policy is defined
   */
  ResponsePolicy getResponsePolicy(CommandArguments commandArguments);
}
