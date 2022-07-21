package redis.clients.jedis.timeseries;

import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.LATEST;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.GET command.
 */
public class TSGetParams implements IParams {

  private boolean latest;

  public static TSGetParams getParams() {
    return new TSGetParams();
  }

  public TSGetParams latest() {
    this.latest = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (latest) {
      args.add(LATEST);
    }
  }
}
