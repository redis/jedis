package redis.clients.jedis.commands;

import redis.clients.jedis.annots.Experimental;

/**
 * String commands for the {@code HIMPORT} command family (Hinted Hash Templates, Redis 8.10).
 * <p>
 * {@code HIMPORT} is a bulk hash-ingestion API: field names are sent once with
 * {@link #himportPrepare(String, String...) HIMPORT PREPARE}, then each hash is created with
 * {@link #himportSet(String, String, String...) HIMPORT SET} sending only values. The server stores
 * such hashes in a memory-efficient template encoding (field names kept once). Keys created this
 * way are ordinary hashes &mdash; every hash command works on them.
 * <p>
 * A fieldset is scoped to the <b>physical connection</b> that prepared it: it is not visible to
 * other connections and is discarded when the connection closes or the client issues {@code RESET}.
 * On the single-connection {@link redis.clients.jedis.Jedis} client the commands can be called
 * directly (its owned connection guarantees affinity); on the pooled/cluster
 * {@link redis.clients.jedis.UnifiedJedis} client they are reached through the connection-holding,
 * {@link AutoCloseable} handle returned by {@link redis.clients.jedis.UnifiedJedis#hashImport()}
 * (used with try-with-resources).
 * <p>
 * Field order, value counts and duplicate fields are server-authoritative; server errors propagate
 * unchanged. Clients must not reorder, deduplicate or sort fields/values.
 * @since 8.0
 */
@Experimental
public interface HashImportCommands {

  /**
   * <b><a href="https://redis.io/commands/himport-prepare">HIMPORT PREPARE</a></b>
   * <p>
   * Defines a session-local fieldset that maps {@code fieldset} to the given field names. The
   * fieldset lives only on the current connection. Re-preparing an existing name is silent
   * (last-writer-wins).
   * <p>
   * Time complexity: O(N*log(N)) where N is the number of fields.
   * @param fieldset the fieldset name
   * @param fields the field names, in the order values will later be supplied to
   *          {@link #himportSet(String, String, String...)}
   * @return {@code OK}
   * @since 8.0
   */
  String himportPrepare(String fieldset, String... fields);

  /**
   * <b><a href="https://redis.io/commands/himport-set">HIMPORT SET</a></b>
   * <p>
   * Creates a hash at {@code key} from {@code values}, positionally paired against the fields of a
   * previously {@link #himportPrepare(String, String...) prepared} fieldset. Fully replaces any
   * existing hash at {@code key}.
   * <p>
   * Time complexity: O(N) where N is the number of fields in the fieldset.
   * @param key the hash key
   * @param fieldset the name of a previously prepared fieldset
   * @param values the values, positionally matching the fieldset's fields
   * @return {@code OK}
   * @since 8.0
   */
  String himportSet(String key, String fieldset, String... values);

  /**
   * <b><a href="https://redis.io/commands/himport-discard">HIMPORT DISCARD</a></b>
   * <p>
   * Removes a single session-local fieldset by name.
   * <p>
   * Time complexity: O(N) where N is the number of session-local fieldsets.
   * @param fieldset the fieldset name
   * @return {@code 1} if the fieldset was removed, {@code 0} if it did not exist
   * @since 8.0
   */
  long himportDiscard(String fieldset);

  /**
   * <b><a href="https://redis.io/commands/himport-discardall">HIMPORT DISCARDALL</a></b>
   * <p>
   * Removes all session-local fieldsets for the current connection.
   * <p>
   * Time complexity: O(N) where N is the number of session-local fieldsets.
   * @return the number of fieldsets removed
   * @since 8.0
   */
  long himportDiscardAll();
}
