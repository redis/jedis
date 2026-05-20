package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

/**
 * Reducer for {@code REDUCE COLLECT}, which gathers per-document projections within a
 * {@code GROUPBY} group, optionally sorted and bounded.
 * <p>
 * The grammar implemented by this reducer is:
 *
 * <pre>{@code
 * REDUCE COLLECT <narg>
 *     FIELDS ( * | <num_fields> <field_1> [<field_2> ...] )
 *     [SORTBY <narg> <@field> [ASC|DESC] [<@field> [ASC|DESC] ...]]
 *     [LIMIT <offset> <count>]
 *   [AS <alias>]
 * }</pre>
 *
 * <h2>Server-side feature flag (required)</h2>
 *
 * COLLECT is currently considered an unstable feature in Redis Search and is gated behind a
 * runtime configuration switch. Callers MUST enable it on the Redis server before issuing
 * aggregations that use this reducer; otherwise the server replies with
 * {@code SEARCH_QUERY_BAD `COLLECT` is unavailable when `ENABLE_UNSTABLE_FEATURES` is off}.
 *
 * <pre>{@code
 * jedis.configSet("search-enable-unstable-features", "yes");
 * }</pre>
 *
 * The flag can also be set permanently in {@code redis.conf} or via the matching module-load
 * argument.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * AggregationBuilder agg = new AggregationBuilder()
 *     .loadAll()
 *     .groupBy("@color",
 *         Reducers.collect()
 *             .fieldsAll()
 *             .sortBy(SortedField.desc("@sweetness"))
 *             .limit(0, 2)
 *             .as("top_fruits"));
 * }</pre>
 *
 * The reducer is marked {@link Experimental} because both the underlying Redis Search feature
 * and this Java surface are subject to change while the server-side rollout is in progress.
 *
 * @see Reducers#collect()
 */
@Experimental
public final class CollectReducer extends Reducer {

  private boolean allFields = false;
  private final List<String> fields = new ArrayList<>();
  private final List<SortedField> sortFields = new ArrayList<>();
  private Integer limitOffset;
  private Integer limitCount;

  CollectReducer() {
    super("COLLECT");
  }

  /**
   * Project the named fields for every document in the group. Use {@code @__key},
   * {@code @__score} or document field names (e.g. {@code @title}).
   * <p>
   * Mutually exclusive with {@link #fieldsAll()}.
   */
  public CollectReducer fields(String... fields) {
    if (this.allFields) {
      throw new IllegalStateException(
          "REDUCE COLLECT cannot mix FIELDS * with explicit field names");
    }
    Collections.addAll(this.fields, fields);
    return this;
  }

  /**
   * Project every field available in the current input row ({@code FIELDS *}).
   * <p>
   * Per the COLLECT specification, {@code *} does not trigger an implicit load — fields must
   * already be in the pipeline (typically via {@code LOAD *} or because they are grouping
   * keys / reducer aliases).
   * <p>
   * Mutually exclusive with {@link #fields(String...)}.
   */
  public CollectReducer fieldsAll() {
    if (!this.fields.isEmpty()) {
      throw new IllegalStateException(
          "REDUCE COLLECT cannot mix FIELDS * with explicit field names");
    }
    this.allFields = true;
    return this;
  }

  /**
   * In-group sort by one or more fields. May be called multiple times to append further sort
   * keys (each call adds to the existing list).
   */
  public CollectReducer sortBy(SortedField... fields) {
    Collections.addAll(this.sortFields, fields);
    return this;
  }

  /** Convenience for {@code sortBy(SortedField.asc(field))}. */
  public CollectReducer sortByAsc(String field) {
    this.sortFields.add(SortedField.asc(field));
    return this;
  }

  /** Convenience for {@code sortBy(SortedField.desc(field))}. */
  public CollectReducer sortByDesc(String field) {
    this.sortFields.add(SortedField.desc(field));
    return this;
  }

  /** Bound the output per group to the first {@code count} entries (offset 0). */
  public CollectReducer limit(int count) {
    return limit(0, count);
  }

  /** Bound the output per group to {@code count} entries starting at {@code offset}. */
  public CollectReducer limit(int offset, int count) {
    if (offset < 0 || count < 0) {
      throw new IllegalArgumentException("LIMIT offset and count must be non-negative");
    }
    this.limitOffset = offset;
    this.limitCount = count;
    return this;
  }

  @Override
  protected List<Object> getOwnArgs() {
    if (!allFields && fields.isEmpty()) {
      throw new IllegalStateException(
          "REDUCE COLLECT requires either fields(...) or fieldsAll() to be configured");
    }

    List<Object> args = new ArrayList<>();
    args.add(SearchKeyword.FIELDS);
    if (allFields) {
      args.add(Protocol.BYTES_ASTERISK);
    } else {
      args.add(fields.size());
      args.addAll(fields);
    }

    if (!sortFields.isEmpty()) {
      args.add(SearchKeyword.SORTBY);
      args.add(sortFields.size() << 1); // 2 tokens per @field/ASC|DESC pair
      for (SortedField sf : sortFields) {
        args.add(sf.getField());
        args.add(sf.getOrder());
      }
    }

    if (limitOffset != null) {
      args.add(SearchKeyword.LIMIT);
      args.add(limitOffset);
      args.add(limitCount);
    }

    return args;
  }
}
