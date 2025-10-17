package redis.clients.jedis;

import java.util.EnumSet;
import java.util.Iterator;
import redis.clients.jedis.args.Rawable;

public class CommandObject<T> {

    /**
     * Command flags based on command flags exposed by Redis.
     * See <a href="https://redis.io/docs/latest/commands/command/#flags">Command flags</a> for more details.
     *
     * Flags description:
     * - READONLY: Command doesn't modify data
     * - WRITE: Command may modify data
     * - DENYOOM: Command may increase memory usage (deny if out of memory)
     * - ADMIN: Administrative command
     * - PUBSUB: Pub/Sub related command
     * - NOSCRIPT: Command not allowed in scripts
     * - RANDOM: Command has random output for same input
     * - SORT_FOR_SCRIPT: Command output needs sorting for scripts
     * - LOADING: Command allowed while database is loading
     * - STALE: Command allowed on stale replicas
     * - SKIP_MONITOR: Command not shown in MONITOR output
     * - SKIP_SLOWLOG: Command not shown in slowlog
     * - ASKING: Command allowed in cluster ASKING state
     * - FAST: Command has O(1) time complexity
     * - MOVABLEKEYS: Command key positions may vary
     * - MODULE: Module command
     * - BLOCKING: Command may block the client
     * - NO_AUTH: Command allowed without authentication
     * - NO_ASYNC_LOADING: Command not allowed during async loading
     * - NO_MULTI: Command not allowed in MULTI/EXEC
     * - NO_MANDATORY_KEYS: Command may work without keys
     * - ALLOW_BUSY: Command allowed when server is busy
     */
  public enum CommandFlag {
      READONLY,
      WRITE,
      DENYOOM,
      ADMIN,
      PUBSUB,
      NOSCRIPT,
      SORT_FOR_SCRIPT,
      LOADING,
      STALE,
      SKIP_MONITOR,
      ASKING,
      FAST,
      MOVABLEKEYS,
      MODULE,
      BLOCKING,
      NO_AUTH,
      NO_ASYNC_LOADING,
      NO_MULTI,
      NO_MANDATORY_KEYS,
      ALLOW_BUSY
  }

  private final CommandArguments arguments;
  private final Builder<T> builder;
  private EnumSet<CommandFlag> flags;

  public CommandObject(CommandArguments args, Builder<T> builder) {
    this.arguments = args;
    this.builder = builder;
    this.flags = EnumSet.noneOf(CommandFlag.class);
  }

  public CommandObject(CommandArguments args, Builder<T> builder, EnumSet<CommandFlag> commandFlags) {
    this.arguments = args;
    this.builder = builder;
    this.flags = commandFlags;
  }

  public CommandArguments getArguments() {
    return arguments;
  }

  public Builder<T> getBuilder() {
    return builder;
  }

  public EnumSet<CommandFlag>  getFlags() {
    return flags;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    for (Rawable e : arguments) {
      hashCode = 31 * hashCode + e.hashCode();
    }
    hashCode = 31 * hashCode + builder.hashCode();
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CommandObject)) {
      return false;
    }

    Iterator<Rawable> e1 = arguments.iterator();
    Iterator<Rawable> e2 = ((CommandObject) o).arguments.iterator();
    while (e1.hasNext() && e2.hasNext()) {
      Rawable o1 = e1.next();
      Rawable o2 = e2.next();
      if (!(o1 == null ? o2 == null : o1.equals(o2))) {
        return false;
      }
    }
    if (e1.hasNext() || e2.hasNext()) {
      return false;
    }

    return builder == ((CommandObject) o).builder;
  }
}
