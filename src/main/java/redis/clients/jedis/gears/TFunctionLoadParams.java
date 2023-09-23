package redis.clients.jedis.gears;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.gears.RedisGearsProtocol.GearsKeyword;
import redis.clients.jedis.params.IParams;

public class TFunctionLoadParams implements IParams {
  private boolean replace = false;
  private String config;

  public static TFunctionLoadParams loadParams() {
    return new TFunctionLoadParams();
  }

  @Override
  public void addParams(CommandArguments args) {
    if (replace) {
      args.add(GearsKeyword.REPLACE.getValue());
    }

    if (config != null && !config.isEmpty()) {
      args.add(GearsKeyword.CONFIG.getValue());
      args.add(config);
    }
  }

  public TFunctionLoadParams replace() {
    this.replace = true;
    return this;
  }

  public TFunctionLoadParams withConfig(String config) {
    this.config = config;
    return this;
  }
}
