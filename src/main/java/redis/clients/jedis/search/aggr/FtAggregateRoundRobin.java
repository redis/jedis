package redis.clients.jedis.search.aggr;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchBuilderFactory;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.JedisRoundRobinBase;

public class FtAggregateRoundRobin extends JedisRoundRobinBase<AggregationResult> {

  private final String indexName;
  private final CommandArguments args;

  public FtAggregateRoundRobin(ConnectionProvider connectionProvider, String indexName, AggregationBuilder aggr) {
    super(connectionProvider, SearchBuilderFactory.SEARCH_AGGREGATION_RESULT_WITH_CURSOR);
    if (!aggr.isWithCursor()) throw new IllegalArgumentException("cursor must be set");
    this.indexName = indexName;
    this.args = new CommandArguments(SearchProtocol.SearchCommand.AGGREGATE).add(this.indexName).addObjects(aggr.getArgs());
  }

  @Override
  protected boolean isIterationCompleted(AggregationResult reply) {
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
}
