package redis.clients.jedis.search;

import java.util.Collection;
import java.util.function.IntFunction;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchResult.SearchResultBuilder;
import redis.clients.jedis.util.JedisCommandIterationBase;

/**
 * Iterator for paginating through FT.SEARCH results in batches.
 * <p>
 * This class provides an iteration mechanism over {@link Document} results from a RediSearch
 * FT.SEARCH command, automatically handling pagination through the result set.
 *
 * @see SearchResult
 * @see Document
 * @deprecated Use {@link redis.clients.jedis.UnifiedJedis#ftSearch(String, String, FTSearchParams)} directly.
 *             This class will be removed in a future release.
 */
@Deprecated
public class FtSearchIteration extends JedisCommandIterationBase<SearchResult, Document> {

  private int batchStart;
  private final int batchSize;
  private final IntFunction<CommandArguments> args;

  /**
   * {@link FTSearchParams#limit(int, int)} will be ignored.
   */
  public FtSearchIteration(ConnectionProvider connectionProvider, int batchSize, String indexName, String query, FTSearchParams params) {
    this(connectionProvider, null, batchSize, indexName, query, params);
  }

  /**
   * {@link Query#limit(java.lang.Integer, java.lang.Integer)} will be ignored.
   */
  public FtSearchIteration(ConnectionProvider connectionProvider, int batchSize, String indexName, Query query) {
    this(connectionProvider, null, batchSize, indexName, query);
  }

  /**
   * {@link FTSearchParams#limit(int, int)} will be ignored.
   */
  public FtSearchIteration(ConnectionProvider connectionProvider, RedisProtocol protocol, int batchSize, String indexName, String query, FTSearchParams params) {
    super(connectionProvider, protocol == RedisProtocol.RESP3 ? SearchResult.SEARCH_RESULT_BUILDER
        : new SearchResultBuilder(!params.getNoContent(), params.getWithScores(), true));
    this.batchSize = batchSize;
    this.args = (limitFirst) -> new CommandArguments(SearchProtocol.SearchCommand.SEARCH)
        .add(indexName).add(query).addParams(params.limit(limitFirst, this.batchSize));
  }

  /**
   * {@link Query#limit(java.lang.Integer, java.lang.Integer)} will be ignored.
   */
  public FtSearchIteration(ConnectionProvider connectionProvider, RedisProtocol protocol, int batchSize, String indexName, Query query) {
    super(connectionProvider, protocol == RedisProtocol.RESP3 ? SearchResult.SEARCH_RESULT_BUILDER
        : new SearchResultBuilder(!query.getNoContent(), query.getWithScores(), true));
    this.batchSize = batchSize;
    this.args = (limitFirst) -> new CommandArguments(SearchProtocol.SearchCommand.SEARCH)
        .add(indexName).addParams(query.limit(limitFirst, this.batchSize));
  }

  @Override
  protected boolean isNodeCompleted(SearchResult reply) {
    return batchStart >= reply.getTotalResults() - batchSize;
  }

  @Override
  protected CommandArguments initCommandArguments() {
    batchStart = 0;
    return args.apply(batchStart);
  }

  @Override
  protected CommandArguments nextCommandArguments(SearchResult lastReply) {
    batchStart += batchSize;
    return args.apply(batchStart);
  }

  @Override
  protected Collection<Document> convertBatchToData(SearchResult batch) {
    return batch.getDocuments();
  }
}
