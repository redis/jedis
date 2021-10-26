package redis.clients.jedis.search;

public interface RediSearchCommands {

  String ftCreate(String indexName, IndexOptions indexOptions, Schema schema);

  SearchResult ftSearch(String indexName, Query query);

  SearchResult ftSearch(byte[] indexName, Query query);
}
