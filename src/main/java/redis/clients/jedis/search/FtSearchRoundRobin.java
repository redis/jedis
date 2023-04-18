package redis.clients.jedis.search;

import java.util.function.IntFunction;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchResult.SearchResultBuilder;
import redis.clients.jedis.util.JedisRoundRobinBase;

public class FtSearchRoundRobin extends JedisRoundRobinBase<SearchResult> {

  private int batchStart;
  private final int batchSize;
  private final IntFunction<CommandArguments> args;

  /**
   * {@link FTSearchParams#limit(int, int)} will be ignored.
   */
  public FtSearchRoundRobin(ConnectionProvider connectionProvider, int batchSize, String indexName, String query, FTSearchParams params) {
    super(connectionProvider, new SearchResultBuilder(!params.getNoContent(), params.getWithScores(), false, true));
    this.batchSize = batchSize;
    this.args = (limitFirst) -> new CommandArguments(SearchProtocol.SearchCommand.SEARCH)
        .add(indexName).add(query).addParams(params.limit(limitFirst, this.batchSize));
  }

  /**
   * {@link Query#limit(java.lang.Integer, java.lang.Integer)} will be ignored.
   */
  public FtSearchRoundRobin(ConnectionProvider connectionProvider, int batchSize, String indexName, Query query) {
    super(connectionProvider, new SearchResultBuilder(!query.getNoContent(), query.getWithScores(), query.getWithPayloads(), true));
    this.batchSize = batchSize;
    this.args = (limitFirst) -> new CommandArguments(SearchProtocol.SearchCommand.SEARCH)
        .add(indexName).addParams(query.limit(limitFirst, this.batchSize));
  }

  @Override
  protected boolean isIterationCompleted(SearchResult reply) {
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
}
