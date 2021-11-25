package redis.clients.jedis.search;

import redis.clients.jedis.Response;

public interface RediSearchPipelineCommands {

  Response<String> ftCreate(String indexName, IndexOptions indexOptions, Schema schema);

  Response<SearchResult> ftSearch(String indexName, Query query);

  Response<SearchResult> ftSearch(byte[] indexName, Query query);
}
