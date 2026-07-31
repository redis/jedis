package redis.clients.jedis;

/**
 * {@code HIMPORT SET} helpers: client-side argument validation, and the per-connection
 * prepare-before-use that a {@code himportSet} {@link CommandObject}'s
 * {@linkplain CommandObject#getPreProcessHook() pre-process hook} runs. The hook is invoked by
 * {@link Connection#executeCommand(CommandObject)} once the {@code CommandExecutor} has picked a
 * connection &mdash; so retry / cluster redirection / failover stay in force &mdash; injecting a
 * {@code PREPARE} on that connection just before the {@code SET} when the fieldset is not yet
 * prepared there.
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
   * Prepare-before-use: if {@code fieldset} is not yet prepared on {@code connection}, send
   * {@code prepareCommand} and record it in the connection's note. The caller must own the
   * connection.
   */
  static void prepareBeforeUse(Connection connection, HashImport fieldset,
      CommandObject<String> prepareCommand) {
    if (!connection.himportIsPrepared(fieldset.name())) {
      connection.executeCommand(prepareCommand);
      connection.himportMarkPrepared(fieldset.name(), fieldset);
    }
  }
}
