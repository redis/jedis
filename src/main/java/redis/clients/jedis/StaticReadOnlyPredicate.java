package redis.clients.jedis;

public class StaticReadOnlyPredicate implements ReadOnlyPredicate {

  private static final StaticReadOnlyPredicate REGISTRY = new StaticReadOnlyPredicate();

  private StaticReadOnlyPredicate() {
  }

  public static StaticReadOnlyPredicate registry() {
    return REGISTRY;
  }

  public boolean isReadOnly(CommandArguments command) {
    return StaticCommandFlagsRegistry.registry().getFlags(command)
        .contains(CommandFlagsRegistry.CommandFlag.READONLY);
  }
}
