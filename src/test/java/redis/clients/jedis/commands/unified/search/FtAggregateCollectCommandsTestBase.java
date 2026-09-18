package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.redis.test.annotations.SinceRedisVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.IndexDataType;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.search.aggr.SortedField;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TagField;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SearchFeatureFlags;

/**
 * Base integration tests for the new {@code REDUCE COLLECT} reducer added to FT.AGGREGATE (see
 * COLLECT.md), exercised through the native {@link AggregationBuilder} / {@link Reducers#collect()}
 * API so they also serve as a contract check on the Java surface.
 * <p>
 * The base class inherits the {@code standalone0} endpoint from
 * {@link UnifiedJedisCommandsTestBase}. The test class skips itself when the Redis Search or JSON
 * modules are not loaded on the target server, and individual tests skip when the running Search
 * build does not yet recognize the COLLECT reducer or one of its sub-features.
 * <p>
 * COLLECT is gated behind {@code search-enable-unstable-features}; the suite flips it on once via
 * {@link SearchFeatureFlags#enableUnstable(redis.clients.jedis.UnifiedJedis)} on the first
 * {@code @BeforeEach}. For cluster topologies the underlying {@code RedisClusterClient.configSet}
 * broadcasts to every primary shard automatically.
 * <p>
 * The reducer was introduced in Redis Search 8.7.225 — the class is annotated with
 * {@link SinceRedisVersion} so the entire suite skips on older servers.
 */
@Tag("search")
@SinceRedisVersion(value = "8.7.225", message = "FT.AGGREGATE REDUCE COLLECT requires Redis Search 8.7.225 or newer")
public abstract class FtAggregateCollectCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String INDEX = "idx:collect_it";
  protected static final String PREFIX = "fruit:";

  private static boolean unstableFlagInitialized;
  private static String previousUnstableFlag;

  public FtAggregateCollectCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  void prepareData() {
    assumeTrue(hasCommand("FT.AGGREGATE"), "Redis Search module is not loaded on " + endpoint);
    assumeTrue(hasCommand("JSON.SET"), "RedisJSON module is not loaded on " + endpoint);

    // Flip the suite-wide flag exactly once per JVM run, via the current test client.
    // RedisClusterClient broadcasts CONFIG SET to every primary shard automatically, so this
    // single call is enough for both standalone and cluster topologies.
    if (!unstableFlagInitialized) {
      previousUnstableFlag = SearchFeatureFlags.enableUnstable(jedis);
      unstableFlagInitialized = true;
    }
    assumeTrue(previousUnstableFlag != null,
      "search-enable-unstable-features is not configurable on this Redis build");

    dropIndexQuietly();

    List<SchemaField> schema = new ArrayList<>();
    schema.add(TagField.of("$.fruit").as("fruit"));
    schema.add(TagField.of("$.color").as("color"));
    schema.add(NumericField.of("$.sweetness").as("sweetness").sortable());
    jedis.ftCreate(INDEX, FTCreateParams.createParams().on(IndexDataType.JSON).prefix(PREFIX),
      schema);

    setJson("fruit:1",
      new JSONObject().put("fruit", "apple").put("color", "yellow").put("sweetness", 6));
    setJson("fruit:2", new JSONObject().put("fruit", "banana").put("color", "yellow")
        .put("sweetness", 5).put("origin", "ecuador"));
    setJson("fruit:3",
      new JSONObject().put("fruit", "apple").put("color", "red").put("sweetness", 7));
    setJson("fruit:4",
      new JSONObject().put("fruit", "banana").put("color", "yellow").put("sweetness", 4));
    setJson("fruit:5",
      new JSONObject().put("fruit", "apple").put("color", "red").put("sweetness", 7));
    // Sparse document — missing the sweetness field.
    setJson("fruit:6", new JSONObject().put("fruit", "cherry").put("color", "red"));

    assumeCollectSupported();
  }

  @Test
  public void collectNamedFieldsReturnsAllMembersOfTheGroup() {
    AggregationBuilder agg = new AggregationBuilder().groupBy("@color",
      Reducers.collect().fields("@fruit", "@sweetness").as("items"));

    AggregationResult result = jedis.ftAggregate(INDEX, agg);
    List<Map<String, Object>> yellowItems = collectedRows(rowFor(result, "color", "yellow"),
      "items");

    // 3 yellow documents: fruit:1, fruit:2, fruit:4
    assertEquals(3, yellowItems.size(), "yellow group must collect every matching document");
    for (Map<String, Object> entry : yellowItems) {
      assertThat(entry, hasKey("fruit"));
      assertThat(entry, hasKey("sweetness"));
    }
  }

  @Test
  public void collectSortByLimitYieldsTopKPerGroup() {
    AggregationBuilder agg = new AggregationBuilder().groupBy("@color",
      Reducers.collect().fields("@fruit", "@sweetness").sortBy(SortedField.desc("@sweetness"))
          .limit(0, 2).as("top"));

    AggregationResult result = jedis.ftAggregate(INDEX, agg);
    List<Map<String, Object>> top = collectedRows(rowFor(result, "color", "yellow"), "top");

    assertEquals(2, top.size(), "LIMIT 0 2 must cap the group at 2 entries");
    // SORTBY @sweetness DESC -> 6 (apple) then 5 (banana)
    assertThat(top.get(0), hasEntry("sweetness", "6"));
    assertThat(top.get(1), hasEntry("sweetness", "5"));
  }

  @Test
  public void collectStarReturnsFullDocumentRow() {
    AggregationBuilder agg = new AggregationBuilder().loadAll().groupBy("@color",
      Reducers.collect().fieldsAll().sortByDesc("@sweetness").limit(2).as("top"));

    AggregationResult result;
    try {
      result = jedis.ftAggregate(INDEX, agg);
    } catch (JedisDataException e) {
      skipIfFeatureNotShipped("COLLECT FIELDS *", e);
      return;
    }

    List<Map<String, Object>> top = collectedRows(rowFor(result, "color", "yellow"), "top");
    assertThat(top, hasSize(lessThanOrEqualTo(2)));

    // For JSON documents the full payload is exposed under the `$` root path; the
    // grouping key (@color) is part of the per-row payload too.
    for (Map<String, Object> entry : top) {
      assertThat(entry, hasKey("color"));
      assertThat(entry, hasKey("$"));
      assertThat(entry.get("$").toString(), containsString("\"sweetness\""));
    }
  }

  @Test
  public void collectEmitsSparseEntriesWhenFieldMissing() {
    AggregationBuilder agg = new AggregationBuilder().groupBy("@color",
      Reducers.collect().fields("@fruit", "@sweetness").as("items"));

    AggregationResult result = jedis.ftAggregate(INDEX, agg);
    List<Map<String, Object>> items = collectedRows(rowFor(result, "color", "red"), "items");

    boolean sawCherryWithoutSweetness = false;
    for (Map<String, Object> entry : items) {
      Object fruit = entry.get("fruit");
      if (fruit != null && "cherry".equals(fruit.toString())) {
        // Sparse semantics: the sweetness key must be omitted entirely, not nulled.
        assertThat(entry, not(hasKey("sweetness")));
        sawCherryWithoutSweetness = true;
      }
    }
    assertThat("Expected at least one sparse entry for cherry", sawCherryWithoutSweetness,
      is(true));
  }

  @Test
  public void collectDeterministicTieBreakByKey() {
    // fruit:3 and fruit:5 both have sweetness=7; appending @__key as the least-significant
    // sort key (COLLECT.md §6.4) makes the order deterministic. @__key isn't in the source
    // row by default — pull it in via LOAD 1 @__key.
    AggregationBuilder agg = new AggregationBuilder().load("@__key").groupBy("@color",
      Reducers.collect().fields("@__key", "@fruit", "@sweetness")
          .sortBy(SortedField.desc("@sweetness"), SortedField.asc("@__key")).limit(0, 2).as("top"));

    AggregationResult result;
    try {
      result = jedis.ftAggregate(INDEX, agg);
    } catch (JedisDataException e) {
      skipIfFeatureNotShipped("COLLECT @__key sort", e);
      return;
    }

    List<Map<String, Object>> top = collectedRows(rowFor(result, "color", "red"), "top");
    assertThat(top, hasSize(lessThanOrEqualTo(2)));

    List<String> keys = new ArrayList<>();
    for (Map<String, Object> entry : top) {
      keys.add(entry.get("__key").toString());
    }
    // sweetness=7 winners are fruit:3 then fruit:5 by ASC key.
    assertThat(keys, contains("fruit:3", "fruit:5"));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private Row rowFor(AggregationResult result, String groupKey, String groupValue) {
    for (Row row : result.getRows()) {
      Object v = row.get(groupKey);
      if (v != null && groupValue.equals(v.toString())) {
        return row;
      }
    }
    throw new AssertionError(
        "No row with " + groupKey + "=" + groupValue + " in " + result.getResults());
  }

  /**
   * Decodes the alias slot produced by REDUCE COLLECT into a list of flat {@code field -> value}
   * maps. Handles RESP2 (flat list) and RESP3 (KeyValue) shapes.
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> collectedRows(Row row, String alias) {
    Object slot = row.get(alias);
    assertThat("alias " + alias + " missing in row", slot, is(not(equalTo(null))));
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object entry : (List<Object>) slot) {
      Map<String, Object> map = new LinkedHashMap<>();
      if (entry instanceof List && !((List<?>) entry).isEmpty()
          && ((List<?>) entry).get(0) instanceof KeyValue) {
        for (KeyValue<Object, Object> kv : (List<KeyValue<Object, Object>>) entry) {
          map.put(kv.getKey().toString(), kv.getValue());
        }
      } else {
        // RESP2 entry: [k, v, k, v, ...]
        List<Object> flat = (List<Object>) entry;
        for (int i = 0; i < flat.size(); i += 2) {
          map.put(flat.get(i).toString(), flat.get(i + 1));
        }
      }
      out.add(map);
    }
    return out;
  }

  private void assumeCollectSupported() {
    AggregationBuilder probe = new AggregationBuilder().groupBy("@color",
      Reducers.collect().fields("@fruit").as("items"));
    try {
      jedis.ftAggregate(INDEX, probe);
    } catch (JedisDataException e) {
      String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
      assumeTrue(
        !msg.contains("unknown") && !msg.contains("syntax") && !msg.contains("unavailable")
            && !msg.contains("invalid argument"),
        "FT.AGGREGATE REDUCE COLLECT not supported by the running Redis Search build: "
            + e.getMessage());
      throw e;
    }
  }

  private void skipIfFeatureNotShipped(String featureLabel, JedisDataException e) {
    String message = e.getMessage() == null ? "" : e.getMessage();
    boolean notYetShipped = message.contains("Unknown argument")
        || message.contains("SEARCH_PROP_NOT_FOUND")
        || message.contains("not loaded nor in pipeline");
    assumeTrue(!notYetShipped,
      featureLabel + " is not yet supported by the running Redis Search build: " + message);
    throw e;
  }

  private boolean hasCommand(String command) {
    try {
      Object reply = jedis.sendCommand(redis.clients.jedis.Protocol.Command.COMMAND, "INFO",
        command);
      if (!(reply instanceof List)) return false;
      List<?> list = (List<?>) reply;
      if (list.isEmpty()) return false;
      // COMMAND INFO returns [null] for unknown commands.
      return list.get(0) != null;
    } catch (Exception ignored) {
      return false;
    }
  }

  private void setJson(String key, JSONObject json) {
    jedis.jsonSet(key, Path2.ROOT_PATH, json);
  }

  private void dropIndexQuietly() {
    try {
      jedis.ftDropIndex(INDEX);
    } catch (Exception ignored) {
      // index didn't exist
    }
  }
}
