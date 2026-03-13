package redis.clients.jedis.executors;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;

/**
 * Connection resolver interface for determining which connection to use for command execution.
 * <p>
 * This interface is used internally by the cluster command executor to resolve connections based on
 * the command type (keyed vs keyless) and read preference configuration.
 */
interface ConnectionResolver {

  /**
   * Intent of a connection request - whether the command is a read or write operation.
   */
  enum ConnectionIntent {
    READ, WRITE
  }

  /**
   * Resolves the appropriate connection for executing the given command.
   * @param cmd the command object to execute
   * @return the connection to use for command execution
   */
  Connection resolve(CommandObject<?> cmd);

  /**
   * Determines the intent (READ or WRITE) for a given command based on its flags.
   * @param command the command object to check
   * @param flags the command flags registry to use for flag lookup
   * @return ConnectionIntent.READ if the command has the READONLY flag, otherwise
   *         ConnectionIntent.WRITE
   */
  default ConnectionIntent getIntent(CommandObject<?> command, CommandFlagsRegistry flags) {
    return flags.getFlags(command.getArguments()).contains(
      CommandFlagsRegistry.CommandFlag.READONLY) ? ConnectionIntent.READ : ConnectionIntent.WRITE;
  }
}
