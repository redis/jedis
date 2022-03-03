package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.REPLACE;
import static redis.clients.jedis.Protocol.Keyword.DESCRIPTION;

import redis.clients.jedis.CommandArguments;

public class FunctionLoadParams implements IParams {
  private boolean replace = false;
  private String description;

  public FunctionLoadParams() { }

  /**
   * overwrites the existing library with the new contents
   */
  public FunctionLoadParams replace() {
    this.replace = true;
    return this;
  }

  /**
   * attach a description to the library
   */
  public FunctionLoadParams libraryDescription(String desc) {
    this.description = desc;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (this.replace) {
      args.add(REPLACE);
    }

    if (this.description != null) {
      args.add(DESCRIPTION);
      args.add(this.description);
    }
  }
}
