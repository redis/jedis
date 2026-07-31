package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Client-side orchestration for {@code HIMPORT SET}: argument validation and prepare-before-use on
 * a single owned/borrowed {@link Connection}. Kept out of {@link Connection} (transport) and shared
 * by {@link UnifiedJedis} (pooled/cluster) and {@link Jedis} (single owned connection).
 */
final class HashImportSupport {

  private HashImportSupport() {
  }

  static void checkArgs(HashImport fieldset, int valueCount) {
    if (fieldset.isDiscarded()) {
      throw new IllegalStateException("HashImport '" + fieldset.name() + "' has been discarded");
    }
    if (valueCount != fieldset.size()) {
      throw new IllegalArgumentException("HashImport '" + fieldset.name() + "' expects "
          + fieldset.size() + " values but got " + valueCount);
    }
  }

  /**
   * Runs {@code setCommand} on {@code connection} with prepare-before-use: if the fieldset is not
   * yet prepared on this connection, inject {@code prepareCommand} first and record it in the
   * connection's note. If the server reports the fieldset missing (session lost via a path the note
   * did not observe), re-prepare and retry the SET exactly once. The caller must own the connection
   * for the whole call.
   */
  static String set(Connection connection, HashImport fieldset,
      CommandObject<String> prepareCommand, CommandObject<String> setCommand) {
    if (!connection.himportIsPrepared(fieldset.name())) {
      prepare(connection, fieldset, prepareCommand);
    }
    try {
      return connection.executeCommand(setCommand);
    } catch (JedisDataException e) {
      if (!isNoSuchFieldset(e)) {
        throw e;
      }
      // The connection lost the fieldset out-of-band; re-prepare on the same socket and retry once.
      prepare(connection, fieldset, prepareCommand);
      return connection.executeCommand(setCommand);
    }
  }

  private static void prepare(Connection connection, HashImport fieldset,
      CommandObject<String> prepareCommand) {
    connection.executeCommand(prepareCommand);
    connection.himportMarkPrepared(fieldset.name(), fieldset);
  }

  private static boolean isNoSuchFieldset(JedisDataException e) {
    String message = e.getMessage();
    return message != null && message.toLowerCase().contains("no such fieldset");
  }
}
