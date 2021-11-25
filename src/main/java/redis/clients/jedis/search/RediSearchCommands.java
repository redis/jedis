package redis.clients.jedis.search;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;

public interface RediSearchCommands {

  String ftCreate(String indexName, IndexOptions indexOptions, Schema schema);

  default String ftAlter(String indexName, Schema.Field... fields) {
    return ftAlter(indexName, Schema.from(fields));
  }

  String ftAlter(String indexName, Schema schema);

  SearchResult ftSearch(String indexName, Query query);

  SearchResult ftSearch(byte[] indexName, Query query);

  String ftExplain(String indexName, Query query);

  List<String> ftExplainCLI(String indexName, Query query);

  AggregationResult ftAggregate(String indexName, AggregationBuilder aggr);

  AggregationResult ftCursorRead(String indexName, long cursorId, int count);

  String ftCursorDel(String indexName, long cursorId);

  String ftDropIndex(String indexName);

  String ftDropIndexDD(String indexName);

  String ftSynUpdate(String indexName, String synonymGroupId, String... terms);

  Map<String, List<String>> ftSynDump(String indexName);

  Map<String, Object> ftInfo(String indexName);

  String ftAliasAdd(String aliasName, String indexName);

  String ftAliasUpdate(String aliasName, String indexName);

  String ftAliasDel(String aliasName);

  Map<String, String> ftConfigGet(String option);

  Map<String, String> ftConfigGet(String indexName, String option);

  String ftConfigSet(String option, String value);

  String ftConfigSet(String indexName, String option, String value);
}
