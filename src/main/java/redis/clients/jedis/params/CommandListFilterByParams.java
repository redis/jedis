package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;

import static redis.clients.jedis.Protocol.Keyword.FILTERBY;
import static redis.clients.jedis.Protocol.Keyword.MODULE;
import static redis.clients.jedis.Protocol.Keyword.ACLCAT;
import static redis.clients.jedis.Protocol.Keyword.PATTERN;

public class CommandListFilterByParams implements IParams{
  private String moduleName;
  private String category;
  private String pattern;

  public static CommandListFilterByParams commandListFilterByParams() {
    return new CommandListFilterByParams();
  }

  public CommandListFilterByParams filterByModule(String moduleName) {
    this.moduleName = moduleName;
    return this;
  }

  public CommandListFilterByParams filterByAclcat(String category) {
    this.category = category;
    return this;
  }

  public CommandListFilterByParams filterByPattern(String pattern) {
    this.pattern = pattern;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(FILTERBY);

    if (moduleName != null) {
      args.add(MODULE);
      args.add(moduleName);
    } else if (category != null) {
      args.add(ACLCAT);
      args.add(category);
    } else if (pattern != null) {
      args.add(PATTERN);
      args.add(pattern);
    }
  }
}
