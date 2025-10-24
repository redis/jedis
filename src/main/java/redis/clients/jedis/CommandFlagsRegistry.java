package redis.clients.jedis;

import java.util.EnumSet;
import redis.clients.jedis.commands.ProtocolCommand;

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
   * Get the flags for a given command.
   * @param cmd the protocol command
   * @return EnumSet of CommandFlag for this command, or empty set if command has no flags
   */
  EnumSet<CommandFlag> getFlags(ProtocolCommand cmd);
}
