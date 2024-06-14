package redis.clients.jedis.timeseries;

/**
 * Represents optional arguments of TS.DECRBY command.
 */
public class TSDecrByParams extends TSArithByParams<TSDecrByParams> {

  public TSDecrByParams() {
  }

  public static TSDecrByParams decrByParams() {
    return new TSDecrByParams();
  }
}
