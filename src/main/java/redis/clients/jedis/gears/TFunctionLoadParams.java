package redis.clients.jedis.gears;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.gears.RedisGearsProtocol.GearsKeyword;
import redis.clients.jedis.params.IParams;

@Deprecated
public class TFunctionLoadParams implements IParams {
  private boolean replace = false;
  private String config;

  public static TFunctionLoadParams loadParams() {
    return new TFunctionLoadParams();
  }

  @Override
  public void addParams(CommandArguments args) {
    if (replace) {
      args.add(GearsKeyword.REPLACE);
    }

    if (config != null && !config.isEmpty()) {
      args.add(GearsKeyword.CONFIG).add(config);
    }
  }

  public TFunctionLoadParams replace() {
    this.replace = true;
    return this;
  }

  public TFunctionLoadParams config(String config) {
    this.config = config;
    return this;
  }
}
