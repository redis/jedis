package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;


public class LolwutParams implements IParams {

  private int version;
  private String[] args;

  public LolwutParams version(int version) {
    this.version = version;
    return this;
  }

  public LolwutParams args(String... args) {
    this.args = args;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (this.version != 0) {
      args.add(this.version);
    }

    if (this.args != null) {
      args.add(this.args);
    }
  }
}
