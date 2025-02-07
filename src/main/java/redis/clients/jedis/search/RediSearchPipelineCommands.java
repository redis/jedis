package redis.clients.jedis.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.schemafields.SchemaField;

public interface RediSearchPipelineCommands {

  Response<String> ftCreate(String indexName, IndexOptions indexOptions, Schema schema);

  default Response<String> ftCreate(String indexName, SchemaField... schemaFields) {
    return ftCreate(indexName, Arrays.asList(schemaFields));
  }

  default Response<String> ftCreate(String indexName, FTCreateParams createParams, SchemaField... schemaFields) {
    return ftCreate(indexName, createParams, Arrays.asList(schemaFields));
  }

  default Response<String> ftCreate(String indexName, Iterable<SchemaField> schemaFields) {
    return ftCreate(indexName, FTCreateParams.createParams(), schemaFields);
  }

  Response<String> ftCreate(String indexName, FTCreateParams createParams, Iterable<SchemaField> schemaFields);

  default Response<String> ftAlter(String indexName, Schema.Field... fields) {
    return ftAlter(indexName, Schema.from(fields));
  }

  Response<String> ftAlter(String indexName, Schema schema);

  default Response<String> ftAlter(String indexName, SchemaField... schemaFields) {
    return ftAlter(indexName, Arrays.asList(schemaFields));
  }

  Response<String> ftAlter(String indexName, Iterable<SchemaField> schemaFields);

  Response<String> ftAliasAdd(String aliasName, String indexName);

  Response<String> ftAliasUpdate(String aliasName, String indexName);

  Response<String> ftAliasDel(String aliasName);

  Response<String> ftDropIndex(String indexName);

  Response<String> ftDropIndexDD(String indexName);

  default Response<SearchResult> ftSearch(String indexName) {
    return ftSearch(indexName, "*");
  }

  Response<SearchResult> ftSearch(String indexName, String query);

  Response<SearchResult> ftSearch(String indexName, String query, FTSearchParams searchParams);

  Response<SearchResult> ftSearch(String indexName, Query query);

  @Deprecated
  Response<SearchResult> ftSearch(byte[] indexName, Query query);

  Response<String> ftExplain(String indexName, Query query);

  Response<List<String>> ftExplainCLI(String indexName, Query query);

  Response<AggregationResult> ftAggregate(String indexName, AggregationBuilder aggr);

  Response<String> ftSynUpdate(String indexName, String synonymGroupId, String... terms);

  Response<Map<String, List<String>>> ftSynDump(String indexName);

  Response<Long> ftDictAdd(String dictionary, String... terms);

  Response<Long> ftDictDel(String dictionary, String... terms);

  Response<Set<String>> ftDictDump(String dictionary);

  Response<Long> ftDictAddBySampleKey(String indexName, String dictionary, String... terms);

  Response<Long> ftDictDelBySampleKey(String indexName, String dictionary, String... terms);

  Response<Set<String>> ftDictDumpBySampleKey(String indexName, String dictionary);

  Response<Map<String, Map<String, Double>>> ftSpellCheck(String index, String query);

  Response<Map<String, Map<String, Double>>> ftSpellCheck(String index, String query,
      FTSpellCheckParams spellCheckParams);

  Response<Map<String, Object>> ftInfo(String indexName);

  Response<Set<String>> ftTagVals(String indexName, String fieldName);

  @Deprecated
  Response<Map<String, Object>> ftConfigGet(String option);

  @Deprecated
  Response<Map<String, Object>> ftConfigGet(String indexName, String option);

  @Deprecated
  Response<String> ftConfigSet(String option, String value);

  @Deprecated
  Response<String> ftConfigSet(String indexName, String option, String value);

  Response<Long> ftSugAdd(String key, String string, double score);

  Response<Long> ftSugAddIncr(String key, String string, double score);

  Response<List<String>> ftSugGet(String key, String prefix);

  Response<List<String>> ftSugGet(String key, String prefix, boolean fuzzy, int max);

  Response<List<Tuple>> ftSugGetWithScores(String key, String prefix);

  Response<List<Tuple>> ftSugGetWithScores(String key, String prefix, boolean fuzzy, int max);

  Response<Boolean> ftSugDel(String key, String string);

  Response<Long> ftSugLen(String key);
}
