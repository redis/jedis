package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;

public class CommandArgumentsTest {

  private enum TestCommand implements ProtocolCommand {
    MSET, HSET;

    @Override
    public byte[] getRaw() {
      return name().getBytes(StandardCharsets.US_ASCII);
    }
  }

  private static List<Object> asList(CommandArguments args) {
    List<Object> values = new ArrayList<>();
    for (Rawable raw : args) {
      values.add(raw.toString());
    }
    return values;
  }

  @Test
  public void preSizedConstructorMatchesDefaultBehavior() {
    CommandArguments expected = new CommandArguments(TestCommand.MSET);
    CommandArguments preSized = new CommandArguments(TestCommand.MSET, 8);
    for (int i = 0; i < 8; i++) {
      String value = "value" + i;
      expected.add(value);
      preSized.add(value);
    }
    assertEquals(asList(expected), asList(preSized));
    assertEquals(expected.getCommand(), preSized.getCommand());
  }

  @Test
  public void preSizedConstructorGrowsBeyondExpectation() {
    CommandArguments args = new CommandArguments(TestCommand.HSET, 1);
    for (int i = 0; i < 100; i++) {
      args.add("value" + i);
    }
    assertEquals(101, asList(args).size()); // command + 100 arguments
  }

  @Test
  public void expectationBelowZeroIsTreatedAsZero() {
    CommandArguments args = new CommandArguments(TestCommand.MSET, -5);
    args.add("a");
    args.add("b");
    assertEquals(3, asList(args).size()); // command + 2 arguments
  }

  @Test
  public void collectionCommandsHonorCommandArgumentsOverride() {
    List<ProtocolCommand> created = new ArrayList<>();
    CommandObjects objects = new CommandObjects(RedisProtocol.RESP3) {
      @Override
      protected CommandArguments commandArguments(ProtocolCommand command) {
        created.add(command);
        return super.commandArguments(command);
      }
    };
    Map<String, String> hash = new HashMap<>();
    hash.put("field", "value");

    objects.hset("key", hash);
    objects.hmset("key", hash);

    // the capacity-aware collection path must still go through the overridable factory
    assertFalse(created.isEmpty());
  }
}
