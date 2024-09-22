package redis.clients.jedis.timeseries;

/**
 * Represents optional arguments of TS.INCRBY command.
 */
public class TSIncrByParams extends TSArithByParams<TSIncrByParams> {

  public TSIncrByParams() {
  }

  public static TSIncrByParams incrByParams() {
    return new TSIncrByParams();
  }
}
