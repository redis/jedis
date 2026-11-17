package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import redis.clients.jedis.util.JedisClusterCRC16;

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

  /** Container commands with a declared subcommand go through the overridable factory too. */
  @Test
  public void containerCommandsHonorCommandArgumentsOverride() {
    class TenantArguments extends CommandArguments {
      TenantArguments(ProtocolCommand command) {
        super(command);
      }

      @Override
      public CommandArguments key(Object key) {
        return super.key("tenant:" + key);
      }
    }
    CommandObjects objects = new CommandObjects(RedisProtocol.RESP3) {
      @Override
      protected CommandArguments commandArguments(ProtocolCommand command) {
        return new TenantArguments(command);
      }
    };

    CommandArguments memoryUsage = objects.memoryUsage("k").getArguments();
    assertTrue(memoryUsage instanceof TenantArguments);
    assertEquals(3, memoryUsage.size());
    assertSame(Protocol.Command.MEMORY, memoryUsage.getCommand());
    assertSame(Protocol.Keyword.USAGE, memoryUsage.get(1));
    // the subcommand is still declared, and the subclass still shapes the key
    assertSame(Protocol.Keyword.USAGE, memoryUsage.getSubcommand());
    assertEquals("tenant:k", memoryUsage.getKeys().get(0));

    CommandArguments xinfoStream = objects.xinfoStream("k").getArguments();
    assertTrue(xinfoStream instanceof TenantArguments);
    assertSame(Protocol.Keyword.STREAM, xinfoStream.getSubcommand());
    assertEquals("tenant:k", xinfoStream.getKeys().get(0));
  }

  /**
   * Without a factory override, the key pre-processor set on CommandObjects still reaches
   * container commands, whose arguments are created through the one-argument factory.
   */
  @Test
  public void containerCommandsApplyTheKeyArgumentPreProcessor() {
    CommandObjects objects = new CommandObjects(RedisProtocol.RESP3);
    objects.setKeyArgumentPreProcessor(key -> "tenant:" + key);

    CommandArguments memoryUsage = objects.memoryUsage("k").getArguments();
    assertSame(Protocol.Keyword.USAGE, memoryUsage.getSubcommand());
    assertEquals("tenant:k", memoryUsage.getKeys().get(0));

    CommandArguments xinfoGroups = objects.xinfoGroups("k").getArguments();
    assertSame(Protocol.Keyword.GROUPS, xinfoGroups.getSubcommand());
    assertEquals("tenant:k", xinfoGroups.getKeys().get(0));

    // a command without a subcommand keeps going through the same pre-processor
    assertEquals("tenant:k", objects.get("k").getArguments().getKeys().get(0));
  }

  /** A null subcommand is rejected and leaves the arguments as they were. */
  @Test
  public void declaringANullSubcommandIsRejected() {
    CommandArguments args = new CommandArguments(Protocol.Command.XINFO);
    assertThrows(IllegalArgumentException.class, () -> args.declareSubcommand(null));
    assertEquals(1, args.size());
    assertNull(args.getSubcommand());
  }

  /** A subcommand can be declared only directly after the command; anything later fails fast. */
  @Test
  public void subcommandIsDeclaredOnlyDirectlyAfterTheCommand() {
    CommandArguments declared = new CommandArguments(Protocol.Command.XINFO)
        .declareSubcommand(Protocol.Keyword.STREAM);
    assertSame(Protocol.Keyword.STREAM, declared.getSubcommand());
    assertEquals(2, declared.size());

    CommandArguments late = new CommandArguments(Protocol.Command.XINFO).add("x");
    assertThrows(IllegalStateException.class,
      () -> late.declareSubcommand(Protocol.Keyword.STREAM));
    assertEquals(2, late.size());

    // already declared at construction: a second one is rejected, not added
    CommandArguments twice = new CommandArguments(Protocol.Command.XINFO, Protocol.Keyword.STREAM);
    assertThrows(IllegalStateException.class,
      () -> twice.declareSubcommand(Protocol.Keyword.GROUPS));
    assertSame(Protocol.Keyword.STREAM, twice.getSubcommand());
    assertEquals(2, twice.size());
  }

  /**
   * A factory override that pre-adds arguments cannot build container commands. CommandObjects
   * builds some of them (SCRIPT FLUSH, SCRIPT KILL) in its field initializers, so the failure
   * already surfaces when such a subclass is instantiated.
   */
  @Test
  public void containerCommandsFailFastWhenTheFactoryPreAddsArguments() {
    assertThrows(IllegalStateException.class, () -> new CommandObjects(RedisProtocol.RESP3) {
      @Override
      protected CommandArguments commandArguments(ProtocolCommand command) {
        return super.commandArguments(command).add("extra");
      }
    });
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

  @Test
  public void binaryKeyIsNotAliasedToTheCallerArray() {
    byte[] key = "foo".getBytes(StandardCharsets.UTF_8);
    CommandArguments args = new CommandArguments(TestCommand.HSET).key(key);

    key[0] = 'b'; // caller reuses its buffer for the next command

    assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), args.get(1).getRaw());
    assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), (byte[]) args.getKeys().get(0));
    assertEquals(JedisClusterCRC16.getSlot("foo"), args.getKeyHashSlots().iterator().next().intValue());
  }

  @Test
  public void routingOnlyBinaryKeyIsNotAliasedToTheCallerArray() {
    byte[] sampleKey = "foo".getBytes(StandardCharsets.UTF_8);
    CommandArguments args = new CommandArguments(TestCommand.HSET).addHashSlotKey(sampleKey);

    sampleKey[0] = 'b'; // caller reuses its buffer for the next command

    assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), (byte[]) args.getKeys().get(0));
    assertEquals(JedisClusterCRC16.getSlot("foo"), args.getKeyHashSlots().iterator().next().intValue());
  }

}
