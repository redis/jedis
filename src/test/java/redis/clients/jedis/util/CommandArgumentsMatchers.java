package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.ArgumentMatcher;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class providing matchers for CommandArguments testing.
 * <p>
 * Provides both Mockito ArgumentMatchers (for mock verification) and Hamcrest Matchers (for
 * assertions).
 * </p>
 * <p>
 * Hamcrest matcher example usage:
 * </p>
 * 
 * <pre>
 * {@code
 * assertThat(args, hasCommand(Protocol.Command.ZRANGE));
 * assertThat(args, hasArgumentCount(3));
 * assertThat(args, hasArgument(1, RawableFactory.from(100L)));
 * assertThat(args, hasArguments(
 *     Protocol.Command.ZRANGE,
 *     RawableFactory.from(0L),
 *     RawableFactory.from(100L)
 * ));
 * }
 * </pre>
 * <p>
 * Mockito matcher example usage:
 * </p>
 * 
 * <pre>
 * {@code
 * verify(mock).someMethod(argThat(commandIs(Protocol.Command.GET)));
 * verify(mock).someMethod(argThat(commandWithArgs(Protocol.Command.SET, "key")));
 * }
 * </pre>
 */
public final class CommandArgumentsMatchers {

  private CommandArgumentsMatchers() {
    throw new InstantiationError("Must not instantiate this class");
  }

  // ========== Mockito ArgumentMatchers ==========

  /**
   * Mockito matcher for CommandArguments with specific ProtocolCommand.
   * @param command the expected protocol command
   * @return an ArgumentMatcher that checks if the CommandArguments has the specified command
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
   * Mockito matcher for CommandArguments containing a specific argument (as String).
   * @param expectedArg the expected argument value (will be compared as String)
   * @return an ArgumentMatcher that checks if the CommandArguments contains the argument
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

  /**
   * Mockito matcher for CommandArguments with specific command and containing a specific argument.
   * @param command the expected protocol command
   * @param expectedArg the expected argument value (will be compared as String)
   * @return an ArgumentMatcher that checks both command and argument
   */
  public static ArgumentMatcher<CommandArguments> commandWithArgs(ProtocolCommand command,
      String expectedArg) {
    return cmd -> commandIs(command).matches(cmd) && hasArgument(expectedArg).matches(cmd);
  }

  // ========== Hamcrest Matchers ==========

  /**
   * Matches CommandArguments with a specific command.
   * @param expectedCommand the expected protocol command
   * @return a matcher that checks if the CommandArguments has the specified command
   */
  public static Matcher<CommandArguments> hasCommand(ProtocolCommand expectedCommand) {
    return new TypeSafeMatcher<CommandArguments>() {
      @Override
      protected boolean matchesSafely(CommandArguments args) {
        return expectedCommand.equals(args.getCommand());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("CommandArguments with command ").appendValue(expectedCommand);
      }

      @Override
      protected void describeMismatchSafely(CommandArguments args,
          Description mismatchDescription) {
        mismatchDescription.appendText("was CommandArguments with command ")
            .appendValue(args.getCommand());
      }
    };
  }

  /**
   * Matches CommandArguments with a specific argument count.
   * @param expectedSize the expected number of arguments (including the command)
   * @return a matcher that checks if the CommandArguments has the specified size
   */
  public static Matcher<CommandArguments> hasArgumentCount(int expectedSize) {
    return new TypeSafeMatcher<CommandArguments>() {
      @Override
      protected boolean matchesSafely(CommandArguments args) {
        return args.size() == expectedSize;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("CommandArguments with argument count ").appendValue(expectedSize);
      }

      @Override
      protected void describeMismatchSafely(CommandArguments args,
          Description mismatchDescription) {
        mismatchDescription.appendText("was CommandArguments with argument count ")
            .appendValue(args.size());
      }
    };
  }

  /**
   * Matches CommandArguments with a specific argument at a specific index.
   * @param index the index of the argument (0-based, where 0 is the command)
   * @param expectedArg the expected Rawable argument
   * @return a matcher that checks if the CommandArguments has the specified argument at the index
   */
  public static Matcher<CommandArguments> hasArgument(int index, Rawable expectedArg) {
    return new TypeSafeMatcher<CommandArguments>() {
      @Override
      protected boolean matchesSafely(CommandArguments args) {
        if (index < 0 || index >= args.size()) {
          return false;
        }
        return expectedArg.equals(args.get(index));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("CommandArguments with argument at index ").appendValue(index)
            .appendText(" equal to ").appendValue(expectedArg);
      }

      @Override
      protected void describeMismatchSafely(CommandArguments args,
          Description mismatchDescription) {
        if (index < 0 || index >= args.size()) {
          mismatchDescription.appendText("index ").appendValue(index)
              .appendText(" is out of bounds (size: ").appendValue(args.size()).appendText(")");
        } else {
          mismatchDescription.appendText("argument at index ").appendValue(index)
              .appendText(" was ").appendValue(args.get(index));
        }
      }
    };
  }

  /**
   * Matches CommandArguments with a specific sequence of arguments.
   * <p>
   * The first argument should be the command, followed by the parameters.
   * </p>
   * @param expectedArgs the expected sequence of Rawable arguments (command + parameters)
   * @return a matcher that checks if the CommandArguments matches the sequence
   */
  public static Matcher<CommandArguments> hasArguments(Rawable... expectedArgs) {
    return new TypeSafeMatcher<CommandArguments>() {
      @Override
      protected boolean matchesSafely(CommandArguments args) {
        if (args.size() != expectedArgs.length) {
          return false;
        }

        Iterator<Rawable> iter = args.iterator();
        for (Rawable expected : expectedArgs) {
          if (!iter.hasNext() || !expected.equals(iter.next())) {
            return false;
          }
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        List<String> decodedExpectedArgs = Arrays.stream(expectedArgs).map(Rawable::getRaw)
            .map(SafeEncoder::encode).collect(Collectors.toList());
        description.appendText("CommandArguments with arguments ").appendValue(decodedExpectedArgs);
      }

      @Override
      protected void describeMismatchSafely(CommandArguments args,
          Description mismatchDescription) {
        List<Rawable> actualArgs = new ArrayList<>();
        args.forEach(actualArgs::add);
        List<String> decodedActualArgs = actualArgs.stream().map(Rawable::getRaw)
            .map(SafeEncoder::encode).collect(Collectors.toList());
        mismatchDescription.appendText("was CommandArguments with arguments ")
            .appendValue(decodedActualArgs);
      }
    };
  }
}
