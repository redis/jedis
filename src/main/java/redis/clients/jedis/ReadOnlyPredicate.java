package redis.clients.jedis;

@FunctionalInterface
public interface ReadOnlyPredicate {

  /**
   * @param command the input command.
   * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
   */
  boolean isReadOnly(CommandArguments command);
}
