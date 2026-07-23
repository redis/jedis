package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Standalone / pooled {@code HIMPORT} handle. Pins a single {@link Connection} borrowed from the
 * provider at construction and runs every {@code HIMPORT} command on it, guaranteeing the fieldset
 * session lives on one socket. {@link #close()} discards remaining fieldsets (if still active) and
 * returns the connection to the pool.
 * @since 8.0
 */
@Experimental
class HashImportHandler extends AbstractHashImportHandler {

  private final Connection connection;

  HashImportHandler(ConnectionProvider provider, CommandObjects commandObjects) {
    super(commandObjects);
    this.connection = provider.getConnection();
  }

  @Override
  public String himportPrepare(String fieldset, String... fields) {
    return guarded(
      () -> connection.executeCommand(commandObjects.himportPrepare(fieldset, fields)));
  }

  @Override
  public String himportSet(String key, String fieldset, String... values) {
    return guarded(
      () -> connection.executeCommand(commandObjects.himportSet(key, fieldset, values)));
  }

  @Override
  public long himportDiscard(String fieldset) {
    return guarded(() -> connection.executeCommand(commandObjects.himportDiscard(fieldset)));
  }

  @Override
  public String himportPrepare(byte[] fieldset, byte[]... fields) {
    return guarded(
      () -> connection.executeCommand(commandObjects.himportPrepare(fieldset, fields)));
  }

  @Override
  public String himportSet(byte[] key, byte[] fieldset, byte[]... values) {
    return guarded(
      () -> connection.executeCommand(commandObjects.himportSet(key, fieldset, values)));
  }

  @Override
  public long himportDiscard(byte[] fieldset) {
    return guarded(() -> connection.executeCommand(commandObjects.himportDiscard(fieldset)));
  }

  @Override
  public long himportDiscardAll() {
    return guarded(() -> connection.executeCommand(commandObjects.himportDiscardAll()));
  }

  @Override
  public void close() {
    try {
      if (state == State.ACTIVE) {
        himportDiscardAll();
      }
    } finally {
      state = State.CLOSED;
      connection.close();
    }
  }
}
