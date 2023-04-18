package redis.clients.jedis.timeseries;

import java.util.List;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.JedisRoundRobinBase;

public class TsMGetRoundRobin extends JedisRoundRobinBase<List<TSKeyValue<TSElement>>> {

  private final CommandArguments args;

  public TsMGetRoundRobin(ConnectionProvider connectionProvider, TSMGetParams multiGetParams, String... filters) {
    super(connectionProvider, TimeSeriesBuilderFactory.TIMESERIES_MGET_RESPONSE);
    this.args = new CommandArguments(TimeSeriesProtocol.TimeSeriesCommand.MGET).addParams(multiGetParams)
        .add(TimeSeriesProtocol.TimeSeriesKeyword.FILTER).addObjects((Object[]) filters);
  }

  @Override
  protected boolean isIterationCompleted(List<TSKeyValue<TSElement>> reply) {
    return reply != null;
  }

  @Override
  protected CommandArguments initCommandArguments() {
    return args;
  }

  @Override
  protected CommandArguments nextCommandArguments(List<TSKeyValue<TSElement>> lastReply) {
    throw new IllegalStateException();
  }
}
