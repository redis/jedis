package redis.clients.jedis.commands;

import redis.clients.jedis.annots.Experimental;

/**
 * Binary ({@code byte[]}) commands for the {@code HIMPORT} command family (Hinted Hash Templates,
 * Redis 8.10).
 * <p>
 * On the pooled/cluster {@link redis.clients.jedis.UnifiedJedis} client these are reached through
 * the same connection-holding, {@link AutoCloseable} handle returned by
 * {@link redis.clients.jedis.UnifiedJedis#hashImport()}.
 * @see HashImportCommands
 * @since 8.0
 */
@Experimental
public interface HashImportBinaryCommands {

  /**
   * Binary variant of {@link HashImportCommands#himportPrepare(String, String...)}.
   * @see HashImportCommands#himportPrepare(String, String...)
   * @since 8.0
   */
  String himportPrepare(byte[] fieldset, byte[]... fields);

  /**
   * Binary variant of {@link HashImportCommands#himportSet(String, String, String...)}.
   * @see HashImportCommands#himportSet(String, String, String...)
   * @since 8.0
   */
  String himportSet(byte[] key, byte[] fieldset, byte[]... values);

  /**
   * Binary variant of {@link HashImportCommands#himportDiscard(String)}.
   * @see HashImportCommands#himportDiscard(String)
   * @since 8.0
   */
  long himportDiscard(byte[] fieldset);

  /**
   * Removes all session-local fieldsets for the current connection.
   * @return the number of fieldsets removed
   * @see HashImportCommands#himportDiscardAll()
   * @since 8.0
   */
  long himportDiscardAll();
}
