package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void ensureCapacityMatchesDefaultBehavior() {
    CommandArguments expected = new CommandArguments(TestCommand.MSET);
    CommandArguments hinted = new CommandArguments(TestCommand.MSET).ensureCapacity(9);
    for (int i = 0; i < 8; i++) {
      String value = "value" + i;
      expected.add(value);
      hinted.add(value);
    }
    assertEquals(asList(expected), asList(hinted));
    assertEquals(expected.getCommand(), hinted.getCommand());
  }

  @Test
  public void ensureCapacityIsChainableMidBuild() {
    CommandArguments args = new CommandArguments(TestCommand.HSET).key("key");
    assertSame(args, args.ensureCapacity(args.size() + 4));
    args.add("f1").add("v1").add("f2").add("v2");
    assertEquals(6, args.size()); // command + key + 4 arguments
  }

  @Test
  public void underEstimatedCapacityGrowsBeyondExpectation() {
    CommandArguments args = new CommandArguments(TestCommand.HSET).ensureCapacity(2);
    for (int i = 0; i < 100; i++) {
      args.add("value" + i);
    }
    assertEquals(101, asList(args).size()); // command + 100 arguments
  }

  @Test
  public void nonPositiveCapacityHasNoEffect() {
    CommandArguments args = new CommandArguments(TestCommand.MSET).ensureCapacity(-5);
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

    // CommandObjects field initialization (cached PING/FLUSHALL/INFO) already invoked
    // the overridden factory; only assert on invocations from the collection commands.
    created.clear();
    objects.hset("key", hash);
    objects.hmset("key", hash);

    // the capacity-aware collection path must still go through the overridable factory
    assertTrue(created.contains(Protocol.Command.HSET));
    assertTrue(created.contains(Protocol.Command.HMSET));
  }

  @Test
  public void addObjectsFromCollectionPreservesAllArguments() {
    CommandArguments args = new CommandArguments(TestCommand.MSET);
    List<String> values = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      values.add("value" + i);
    }

    args.addObjects(values);

    assertEquals(51, asList(args).size()); // command + 50 arguments
  }

  @Test
  public void keysVarargsPreservesAllKeysAndArguments() {
    Object[] keys = new Object[50];
    for (int i = 0; i < 50; i++) {
      keys[i] = "key" + i;
    }

    CommandArguments args = new CommandArguments(TestCommand.MSET).keys(keys);

    assertEquals(51, args.size()); // command + 50 keys
    assertEquals(50, args.getKeys().size());
  }

  @Test
  public void keysCollectionPreservesAllKeysAndArguments() {
    List<String> keys = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      keys.add("key" + i);
    }

    CommandArguments args = new CommandArguments(TestCommand.MSET).keys(keys);

    assertEquals(51, args.size()); // command + 50 keys
    assertEquals(50, args.getKeys().size());
  }

  @Test
  public void addObjectsVarargsPreservesAllArguments() {
    CommandArguments args = new CommandArguments(TestCommand.MSET);
    Object[] values = new Object[50];
    for (int i = 0; i < 50; i++) {
      values[i] = "value" + i;
    }

    args.addObjects(values);

    assertEquals(51, asList(args).size()); // command + 50 arguments
  }

}
