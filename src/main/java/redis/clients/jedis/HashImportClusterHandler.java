package redis.clients.jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Cluster {@code HIMPORT} handle. Pins one {@link Connection} per master at construction (from
 * {@link ClusterConnectionProvider#getPrimaryNodes()}) and reuses them for the whole session
 * &mdash; never borrowing fresh connections, so the connection-scoped fieldsets survive.
 * <p>
 * {@code PREPARE} / {@code DISCARD} / {@code DISCARDALL} fan out to every pinned master (each holds
 * an identical copy of the session-local fieldsets); {@code SET} routes by the (post key
 * preprocessing) hash slot of its key to the pinned connection for that master. A slot that maps
 * off the pinned set (resharding) is session-fatal.
 * @since 8.0
 */
@Experimental
class HashImportClusterHandler extends AbstractHashImportHandler {

  private final ClusterConnectionProvider provider;
  private final Map<HostAndPort, Connection> pinned = new HashMap<>();

  HashImportClusterHandler(ClusterConnectionProvider provider, CommandObjects commandObjects) {
    super(commandObjects);
    this.provider = provider;
    // A pool may fail to hand out a connection (e.g. exhausted) after earlier ones were borrowed;
    // the constructor would then abort before a handle is returned, so close() would never run.
    // Roll back the already-pinned connections on failure to avoid leaking them from their pools.
    for (Map.Entry<String, ConnectionPool> entry : provider.getPrimaryNodes().entrySet()) {
      try {
        pinned.put(HostAndPort.from(entry.getKey()), entry.getValue().getResource());
      } catch (RuntimeException e) {
        pinned.values().forEach(c -> {
          try {
            c.close();
          } catch (RuntimeException ignored) {
            // best-effort release
          }
        });
        pinned.clear();
        throw e;
      }
    }
  }

  @Override
  public String himportPrepare(String fieldset, String... fields) {
    return guarded(
      () -> broadcast(c -> c.executeCommand(commandObjects.himportPrepare(fieldset, fields))));
  }

  @Override
  public String himportSet(String key, String fieldset, String... values) {
    return guarded(() -> {
      CommandObject<String> command = commandObjects.himportSet(key, fieldset, values);
      return connectionForSlot(slotOf(command)).executeCommand(command);
    });
  }

  @Override
  public long himportDiscard(String fieldset) {
    return guarded(() -> aggregate(c -> c.executeCommand(commandObjects.himportDiscard(fieldset))));
  }

  @Override
  public String himportPrepare(byte[] fieldset, byte[]... fields) {
    return guarded(
      () -> broadcast(c -> c.executeCommand(commandObjects.himportPrepare(fieldset, fields))));
  }

  @Override
  public String himportSet(byte[] key, byte[] fieldset, byte[]... values) {
    return guarded(() -> {
      CommandObject<String> command = commandObjects.himportSet(key, fieldset, values);
      return connectionForSlot(slotOf(command)).executeCommand(command);
    });
  }

  @Override
  public long himportDiscard(byte[] fieldset) {
    return guarded(() -> aggregate(c -> c.executeCommand(commandObjects.himportDiscard(fieldset))));
  }

  @Override
  public long himportDiscardAll() {
    return guarded(() -> aggregate(c -> c.executeCommand(commandObjects.himportDiscardAll())));
  }

  @Override
  public void close() {
    state = State.CLOSED;
    // Best-effort DISCARDALL on every pinned connection so no prepared, connection-scoped fieldset
    // is left behind for the next borrower, then return each connection to its pool. This runs even
    // for a BROKEN session (e.g. a SET got MOVED/ASK): a redirection is a data error, not a
    // connection error, so the pinned connections are still alive and worth reusing. A connection
    // that is genuinely dead makes DISCARDALL throw and is already flagged broken internally, so
    // close() hands it back as broken and the pool discards it.
    for (Connection c : pinned.values()) {
      try {
        c.executeCommand(commandObjects.himportDiscardAll());
      } catch (RuntimeException ignored) {
        // best-effort cleanup
      } finally {
        c.close();
      }
    }
    pinned.clear();
  }

  /**
   * Hash slot of the (single-key) command, derived from its built arguments so that any configured
   * key preprocessor (e.g. prefixed keys) is honored &mdash; matching how the regular cluster
   * executor routes.
   */
  private int slotOf(CommandObject<?> command) {
    Set<Integer> slots = command.getArguments().getKeyHashSlots();
    if (slots.size() != 1) {
      state = State.BROKEN;
      throw new JedisException("HashImport SET must target exactly one hash slot, got " + slots);
    }
    return slots.iterator().next();
  }

  /** Runs {@code op} on the pinned connection for {@code slot}, failing the session if it moved. */
  private Connection connectionForSlot(int slot) {
    Connection c = pinned.get(provider.getNode(slot));
    if (c == null) {
      state = State.BROKEN;
      throw new JedisException("HashImport: slot " + slot + " moved off the pinned masters");
    }
    return c;
  }

  /** Fans {@code op} out to all pinned masters; replies are identical, returns {@code OK}. */
  private String broadcast(Function<Connection, String> op) {
    pinned.values().forEach(op::apply);
    return "OK";
  }

  /**
   * Fans {@code op} out to all pinned masters and returns the maximum reply. Every master mirrors
   * the same fieldset state so the replies should agree; the maximum degrades gracefully if one
   * master missed a {@code PREPARE}.
   */
  private long aggregate(Function<Connection, Long> op) {
    long result = 0L;
    for (Connection c : pinned.values()) {
      result = Math.max(result, op.apply(c));
    }
    return result;
  }
}
