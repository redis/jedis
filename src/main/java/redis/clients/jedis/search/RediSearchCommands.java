package redis.clients.jedis.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.commands.ConfigCommands;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.schemafields.SchemaField;

public interface RediSearchCommands {

  String ftCreate(String indexName, IndexOptions indexOptions, Schema schema);

  default String ftCreate(String indexName, SchemaField... schemaFields) {
    return ftCreate(indexName, Arrays.asList(schemaFields));
  }

  default String ftCreate(String indexName, FTCreateParams createParams, SchemaField... schemaFields) {
    return ftCreate(indexName, createParams, Arrays.asList(schemaFields));
  }

  default String ftCreate(String indexName, Iterable<SchemaField> schemaFields) {
    return ftCreate(indexName, FTCreateParams.createParams(), schemaFields);
  }

  String ftCreate(String indexName, FTCreateParams createParams, Iterable<SchemaField> schemaFields);

  default String ftAlter(String indexName, Schema.Field... fields) {
    return ftAlter(indexName, Schema.from(fields));
  }

  String ftAlter(String indexName, Schema schema);

  default String ftAlter(String indexName, SchemaField... schemaFields) {
    return ftAlter(indexName, Arrays.asList(schemaFields));
  }

  String ftAlter(String indexName, Iterable<SchemaField> schemaFields);

  String ftAliasAdd(String aliasName, String indexName);

  String ftAliasUpdate(String aliasName, String indexName);

  String ftAliasDel(String aliasName);

  String ftDropIndex(String indexName);

  String ftDropIndexDD(String indexName);

  default SearchResult ftSearch(String indexName) {
    return ftSearch(indexName, "*");
  }

  SearchResult ftSearch(String indexName, String query);

  SearchResult ftSearch(String indexName, String query, FTSearchParams params);

  SearchResult ftSearch(String indexName, Query query);

  @Deprecated
  SearchResult ftSearch(byte[] indexName, Query query);

  String ftExplain(String indexName, Query query);

  List<String> ftExplainCLI(String indexName, Query query);

  AggregationResult ftAggregate(String indexName, AggregationBuilder aggr);

  AggregationResult ftCursorRead(String indexName, long cursorId, int count);

  String ftCursorDel(String indexName, long cursorId);

  Map.Entry<AggregationResult, ProfilingInfo> ftProfileAggregate(String indexName,
      FTProfileParams profileParams, AggregationBuilder aggr);

  Map.Entry<SearchResult, ProfilingInfo> ftProfileSearch(String indexName,
      FTProfileParams profileParams, Query query);

  Map.Entry<SearchResult, ProfilingInfo> ftProfileSearch(String indexName,
      FTProfileParams profileParams, String query, FTSearchParams searchParams);

  String ftSynUpdate(String indexName, String synonymGroupId, String... terms);

  Map<String, List<String>> ftSynDump(String indexName);

  long ftDictAdd(String dictionary, String... terms);

  long ftDictDel(String dictionary, String... terms);

  Set<String> ftDictDump(String dictionary);

  long ftDictAddBySampleKey(String indexName, String dictionary, String... terms);

  long ftDictDelBySampleKey(String indexName, String dictionary, String... terms);

  Set<String> ftDictDumpBySampleKey(String indexName, String dictionary);

  Map<String, Map<String, Double>> ftSpellCheck(String index, String query);

  Map<String, Map<String, Double>> ftSpellCheck(String index, String query,
      FTSpellCheckParams spellCheckParams);

  Map<String, Object> ftInfo(String indexName);

  Set<String> ftTagVals(String indexName, String fieldName);

  /**
   * @deprecated {@link ConfigCommands#configGet(java.lang.String)} is used since Redis 8.
   */
  @Deprecated
  Map<String, Object> ftConfigGet(String option);

  @Deprecated
  Map<String, Object> ftConfigGet(String indexName, String option);

  /**
   * @deprecated {@link ConfigCommands#configSet(java.lang.String, java.lang.String)} is used since Redis 8.
   */
  @Deprecated
  String ftConfigSet(String option, String value);

  @Deprecated
  String ftConfigSet(String indexName, String option, String value);

  long ftSugAdd(String key, String string, double score);

  long ftSugAddIncr(String key, String string, double score);

  List<String> ftSugGet(String key, String prefix);

  List<String> ftSugGet(String key, String prefix, boolean fuzzy, int max);

  List<Tuple> ftSugGetWithScores(String key, String prefix);

  List<Tuple> ftSugGetWithScores(String key, String prefix, boolean fuzzy, int max);

  boolean ftSugDel(String key, String string);

  long ftSugLen(String key);

  Set<String> ftList();
}
