package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for ClusterCommandObjects class.
 */
public class ClusterCommandObjectsTest {

  private ClusterCommandObjects clusterCommandObjects;

  @BeforeEach
  public void setUp() {
    clusterCommandObjects = new ClusterCommandObjects();
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_singleSlot() {
    String[] keysValues = { "{user}:1", "value1", "{user}:2", "value2", "{user}:3", "value3" };

    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);

    assertEquals(1, result.size());
    assertEquals(Protocol.Command.MSET, result.get(0).getCommand());

    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("value1"));
    assertTrue(argsStrings.contains("{user}:2"));
    assertTrue(argsStrings.contains("value2"));
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_multipleSlots() {
    String[] keysValues = { "key1", "value1", "key2", "value2", "key3", "value3" };

    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);

    Map<Integer, List<String>> expectedSlots = new HashMap<>();
    for (int i = 0; i < keysValues.length; i += 2) {
      int slot = JedisClusterCRC16.getSlot(keysValues[i]);
      expectedSlots.computeIfAbsent(slot, k -> new ArrayList<>()).add(keysValues[i]);
      expectedSlots.get(slot).add(keysValues[i + 1]);
    }

    assertEquals(expectedSlots.size(), result.size());
    for (CommandArguments cmdArgs : result) {
      assertEquals(Protocol.Command.MSET, cmdArgs.getCommand());
    }
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_withParams() {
    String[] keysValues = { "{test}:key1", "value1", "{test}:key2", "value2" };
    CommandArguments args = new CommandArguments(Protocol.Command.SET);
    SetParams params = SetParams.setParams().ex(100);

    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, params);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.stream().anyMatch(s -> s.equalsIgnoreCase("EX")));
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_emptyKeysValues() {
    String[] keysValues = {};
    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);
    assertEquals(0, result.size());
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_oddNumberOfElements() {
    String[] keysValues = { "key1", "value1", "key2" };
    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    assertThrows(IllegalArgumentException.class,
      () -> clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args, keysValues, null));
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlot_nullParams() {
    String[] keysValues = { "{slot}:key1", "value1" };
    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  // Tests for byte[] version

  @Test
  public void testGroupArgumentsByKeyValueHashSlotBinary_singleSlot() {
    byte[][] keysValues = { "{user}:1".getBytes(), "value1".getBytes(), "{user}:2".getBytes(),
        "value2".getBytes(), "{user}:3".getBytes(), "value3".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);

    assertEquals(1, result.size());
    assertEquals(Protocol.Command.MSET, result.get(0).getCommand());

    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("value1"));
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlotBinary_multipleSlots() {
    byte[][] keysValues = { "key1".getBytes(), "value1".getBytes(), "key2".getBytes(),
        "value2".getBytes(), "key3".getBytes(), "value3".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);

    Map<Integer, List<byte[]>> expectedSlots = new HashMap<>();
    for (int i = 0; i < keysValues.length; i += 2) {
      int slot = JedisClusterCRC16.getSlot(keysValues[i]);
      expectedSlots.computeIfAbsent(slot, k -> new ArrayList<>()).add(keysValues[i]);
    }

    assertEquals(expectedSlots.size(), result.size());
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlotBinary_emptyKeysValues() {
    byte[][] keysValues = {};
    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);
    assertEquals(0, result.size());
  }

  @Test
  public void testGroupArgumentsByKeyValueHashSlotBinary_oddNumberOfElements() {
    byte[][] keysValues = { "key1".getBytes(), "value1".getBytes(), "key2".getBytes() };
    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    assertThrows(IllegalArgumentException.class,
      () -> clusterCommandObjects.groupArgumentsByKeyValueHashSlot(args, keysValues, null));
  }

  // Tests for groupArgumentsByKeyHashSlot (String version)

  @Test
  public void testGroupArgumentsByKeyHashSlot_singleSlot() {
    String[] keys = { "{user}:1", "{user}:2", "{user}:3" };

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    assertEquals(Protocol.Command.DEL, result.get(0).getCommand());

    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("{user}:2"));
    assertTrue(argsStrings.contains("{user}:3"));
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_multipleSlots() {
    String[] keys = { "key1", "key2", "key3" };

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    Map<Integer, List<String>> expectedSlots = new HashMap<>();
    for (String key : keys) {
      int slot = JedisClusterCRC16.getSlot(key);
      expectedSlots.computeIfAbsent(slot, k -> new ArrayList<>()).add(key);
    }

    assertEquals(expectedSlots.size(), result.size());
    for (CommandArguments cmdArgs : result) {
      assertEquals(Protocol.Command.DEL, cmdArgs.getCommand());
    }
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_withParams() {
    String[] keys = { "{test}:key1", "{test}:key2" };
    CommandArguments args = new CommandArguments(Protocol.Command.EXISTS);
    SetParams params = SetParams.setParams().ex(100);

    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      params);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.stream().anyMatch(s -> s.equalsIgnoreCase("EX")));
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_emptyKeys() {
    String[] keys = {};
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);
    assertEquals(0, result.size());
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_singleKey() {
    String[] keys = { "singleKey" };
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("singleKey"));
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_nullParams() {
    String[] keys = { "{slot}:key1" };
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGroupArgumentsByKeyHashSlot_noValuesIncluded() {
    // Verify that key-only method doesn't include any extra elements
    String[] keys = { "{user}:1", "{user}:2" };

    CommandArguments args = new CommandArguments(Protocol.Command.MGET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    // Should contain the command (MGET) and the 2 keys, no values
    assertEquals(3, argsStrings.size());
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("{user}:2"));
  }

  // Tests for groupArgumentsByKeyHashSlot (byte[] version)

  @Test
  public void testGroupArgumentsByKeyHashSlotBinary_singleSlot() {
    byte[][] keys = { "{user}:1".getBytes(), "{user}:2".getBytes(), "{user}:3".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    assertEquals(Protocol.Command.DEL, result.get(0).getCommand());

    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("{user}:2"));
    assertTrue(argsStrings.contains("{user}:3"));
  }

  @Test
  public void testGroupArgumentsByKeyHashSlotBinary_multipleSlots() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes(), "key3".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    Map<Integer, List<byte[]>> expectedSlots = new HashMap<>();
    for (byte[] key : keys) {
      int slot = JedisClusterCRC16.getSlot(key);
      expectedSlots.computeIfAbsent(slot, k -> new ArrayList<>()).add(key);
    }

    assertEquals(expectedSlots.size(), result.size());
  }

  @Test
  public void testGroupArgumentsByKeyHashSlotBinary_emptyKeys() {
    byte[][] keys = {};
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);
    assertEquals(0, result.size());
  }

  @Test
  public void testGroupArgumentsByKeyHashSlotBinary_singleKey() {
    byte[][] keys = { "singleKey".getBytes() };
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("singleKey"));
  }

  @Test
  public void testGroupArgumentsByKeyHashSlotBinary_noValuesIncluded() {
    // Verify that key-only method doesn't include any extra elements
    byte[][] keys = { "{user}:1".getBytes(), "{user}:2".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.MGET);
    List<CommandArguments> result = clusterCommandObjects.groupArgumentsByKeyHashSlot(args, keys,
      null);

    assertEquals(1, result.size());
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    // Should contain the command (MGET) and the 2 keys, no values
    assertEquals(3, argsStrings.size());
    assertTrue(argsStrings.contains("{user}:1"));
    assertTrue(argsStrings.contains("{user}:2"));
  }

  private List<String> extractArgsAsStrings(CommandArguments cmdArgs) {
    List<String> result = new ArrayList<>();
    for (Rawable rawable : cmdArgs) {
      result.add(SafeEncoder.encode(rawable.getRaw()));
    }
    return result;
  }
}
