package redis.clients.jedis.util;

import org.mockito.ArgumentMatcher;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;

/**
 * Utility class providing Mockito ArgumentMatchers for CommandArguments testing.
 */
public final class CommandArgumentMatchers {

  private CommandArgumentMatchers() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Matcher for CommandArguments with specific ProtocolCommand
   */
  public static ArgumentMatcher<CommandArguments> commandIs(ProtocolCommand command) {
    return args -> {
      if (args == null || !(args instanceof CommandArguments)) {
        return false;
      }
      return command.equals(args.getCommand());
    };
  }

  /**
   * Matcher for CommandArguments containing specific arguments
   */
  public static ArgumentMatcher<CommandArguments> hasArgument(String expectedArg) {
    return args -> {
      for (Rawable arg : args) {

        if (expectedArg.equals(SafeEncoder.encode(arg.getRaw()))) {
          return true;
        }
      }
      return false;
    };
  }

  public static ArgumentMatcher<CommandArguments> commandWithArgs(ProtocolCommand command,
      String expectedArg) {
    return cmd -> commandIs(command).matches(cmd) && hasArgument(expectedArg).matches(cmd);
  }

}