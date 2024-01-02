package redis.clients.jedis.search.aggr;

import java.util.Collection;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.JedisCommandIterationBase;

public class FtAggregateIteration extends JedisCommandIterationBase<AggregationResult, Row> {

  private final String indexName;
  private final CommandArguments args;

  /**
   * {@link AggregationBuilder#cursor(int, long) CURSOR} must be set.
   * @param connectionProvider connection provider
   * @param indexName index name
   * @param aggr cursor must be set
   */
  public FtAggregateIteration(ConnectionProvider connectionProvider, String indexName, AggregationBuilder aggr) {
    super(connectionProvider, AggregationResult.SEARCH_AGGREGATION_RESULT_WITH_CURSOR);
    if (!aggr.isWithCursor()) throw new IllegalArgumentException("cursor must be set");
    this.indexName = indexName;
    this.args = new CommandArguments(SearchProtocol.SearchCommand.AGGREGATE).add(this.indexName).addParams(aggr);
  }

  @Override
  protected boolean isNodeCompleted(AggregationResult reply) {
    return reply.getCursorId() == 0L;
  }

  @Override
  protected CommandArguments initCommandArguments() {
    return args;
  }

  @Override
  protected CommandArguments nextCommandArguments(AggregationResult lastReply) {
    return new CommandArguments(SearchProtocol.SearchCommand.CURSOR).add(SearchProtocol.SearchKeyword.READ)
        .add(indexName).add(lastReply.getCursorId());
  }

  @Override
  protected Collection<Row> convertBatchToData(AggregationResult batch) {
    return batch.getRows();
  }
}
