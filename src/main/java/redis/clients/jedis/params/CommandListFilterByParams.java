package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class CommandListFilterByParams implements IParams {

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

  public CommandListFilterByParams filterByAclCat(String category) {
    this.category = category;
    return this;
  }

  public CommandListFilterByParams filterByPattern(String pattern) {
    this.pattern = pattern;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(Keyword.FILTERBY);

    if (moduleName != null && category == null && pattern == null) {
      args.add(Keyword.MODULE);
      args.add(moduleName);
    } else if (moduleName == null && category != null && pattern == null) {
      args.add(Keyword.ACLCAT);
      args.add(category);
    } else if (moduleName == null && category == null && pattern != null) {
      args.add(Keyword.PATTERN);
      args.add(pattern);
    } else {
      throw new IllegalArgumentException("Must choose exactly one filter in "
          + getClass().getSimpleName());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommandListFilterByParams that = (CommandListFilterByParams) o;
    return Objects.equals(moduleName, that.moduleName) && Objects.equals(category, that.category) && Objects.equals(pattern, that.pattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleName, category, pattern);
  }
}
