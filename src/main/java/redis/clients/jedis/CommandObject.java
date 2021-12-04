package redis.clients.jedis;

public class CommandObject<T> {

  private final CommandArguments arguments;
  private final Builder<T> builder;

  public CommandObject(CommandArguments args, Builder<T> builder) {
    this.arguments = args;
    this.builder = builder;
  }

  public CommandArguments getArguments() {
    return arguments;
  }

  public Builder<T> getBuilder() {
    return builder;
  }
}
