package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class LolwutParams implements IParams {

  private Integer version;
  private String[] opargs;

  public LolwutParams version(int version) {
    this.version = version;
    return this;
  }

  @Deprecated
  public LolwutParams args(String... args) {
    return optionalArguments(args);
  }

  public LolwutParams optionalArguments(String... args) {
    this.opargs = args;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (version != null) {
      args.add(Keyword.VERSION).add(version);

      if (opargs != null && opargs.length > 0) {
        args.addObjects((Object[]) opargs);
      }
    }
  }
}
