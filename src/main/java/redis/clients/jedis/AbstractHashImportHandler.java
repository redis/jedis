package redis.clients.jedis;

import java.util.function.Supplier;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.commands.HashImportBinaryCommands;
import redis.clients.jedis.commands.HashImportCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisRedirectionException;

/**
 * Base for the pooled/cluster {@code HIMPORT} handles that implement both
 * {@link HashImportCommands} and {@link HashImportBinaryCommands}. Holds the {@link CommandObjects}
 * factory and a one-way session state machine: {@code ACTIVE -> BROKEN} (session-fatal error) or
 * {@code ACTIVE -> CLOSED} ({@link #close()}). There is no recovery.
 * <p>
 * Session-fatal errors ({@link JedisConnectionException} socket death,
 * {@link JedisRedirectionException} {@code MOVED}/{@code ASK} topology change) void the
 * pinned-connection assumption and flip the handle to {@code BROKEN}. Command-level errors
 * ({@code WRONGTYPE}, value-count mismatch, duplicate field, arity, {@code no such fieldset})
 * propagate but leave the handle {@code ACTIVE}.
 * @since 8.0
 */
@Experimental
public abstract class AbstractHashImportHandler
    implements HashImportCommands, HashImportBinaryCommands, AutoCloseable {

  protected enum State {
    ACTIVE, BROKEN, CLOSED
  }

  protected State state = State.ACTIVE;
  protected final CommandObjects commandObjects;

  protected AbstractHashImportHandler(CommandObjects commandObjects) {
    this.commandObjects = commandObjects;
  }

  /**
   * Guards {@code exec} on entry: runs only while {@code ACTIVE}, and flips the handle to
   * {@code BROKEN} on session-fatal errors. Command-level errors are not caught here, so they
   * propagate and leave the handle {@code ACTIVE}.
   */
  protected <T> T guarded(Supplier<T> exec) {
    if (state != State.ACTIVE) {
      throw new JedisException("HashImport session is " + state);
    }
    try {
      return exec.get();
    } catch (JedisConnectionException | JedisRedirectionException fatal) {
      state = State.BROKEN;
      throw fatal;
    }
  }

  /**
   * Discards remaining fieldsets (if the session is still active) and releases the held
   * connection(s) back to the pool; afterwards the handle refuses all further use. Narrows
   * {@link AutoCloseable#close()} to throw no checked exception.
   */
  @Override
  public abstract void close();
}
