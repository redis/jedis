package redis.clients.jedis.timeseries;

import java.util.List;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.JedisRoundRobinBase;

public class TsMRangeRoundRobin extends JedisRoundRobinBase<List<TSKeyedElements>> {

  private final CommandArguments args;

  /**
   * @param connectionProvider connection provider
   * @param reverse {@code false} means TS.MRANGE command; {@code true} means TS.MREVRANGE command
   * @param multiRangeParams optional arguments and parameters
   */
  public TsMRangeRoundRobin(ConnectionProvider connectionProvider, boolean reverse, TSMRangeParams multiRangeParams) {
    super(connectionProvider, TimeSeriesBuilderFactory.TIMESERIES_MRANGE_RESPONSE);
    this.args = new CommandArguments(!reverse ? TimeSeriesCommand.MRANGE : TimeSeriesCommand.MREVRANGE).addParams(multiRangeParams);
  }

  @Override
  protected boolean isIterationCompleted(List<TSKeyedElements> reply) {
    return reply != null;
  }

  @Override
  protected CommandArguments initCommandArguments() {
    return args;
  }

  @Override
  protected CommandArguments nextCommandArguments(List<TSKeyedElements> lastReply) {
    throw new IllegalStateException();
  }
}
