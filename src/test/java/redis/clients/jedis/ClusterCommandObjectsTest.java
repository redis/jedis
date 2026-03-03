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

  // ==================== Rawable Key Hash Slot Tests ====================
  // These tests verify that Rawable keys are handled correctly in hash slot computation.
  // Bug: getKeyHashSlots() only handles byte[] and String keys, but key(Object) also accepts
  // Rawable instances. When getKeyHashSlots() encounters a Rawable key, it falls to the else
  // branch and casts to String, throwing ClassCastException.

  /**
   * Test that using a Rawable key (created via RawableFactory.from(byte[])) throws
   * ClassCastException when computing hash slots. This demonstrates the bug where the old code
   * called processKey(raw.getRaw()) to extract the byte array before slot computation, but the new
   * code stores the original Rawable object without extracting raw bytes.
   */
  @Test
  public void testGetKeyHashSlots_withRawableFromByteArray_throwsClassCastException() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("testkey".getBytes());

    // Using key() with a Rawable should store the Rawable object in the keys list
    args.key(rawableKey);

    // getKeyHashSlots() only handles byte[] and String, not Rawable
    // This should throw ClassCastException because it tries to cast Rawable to String
    assertThrows(ClassCastException.class, () -> args.getKeyHashSlots(),
      "Expected ClassCastException when computing hash slots for Rawable key");
  }

  /**
   * Test that using a Rawable key (created via RawableFactory.from(String)) throws
   * ClassCastException when computing hash slots.
   */
  @Test
  public void testGetKeyHashSlots_withRawableFromString_throwsClassCastException() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("testkey");

    args.key(rawableKey);

    // This should throw ClassCastException
    assertThrows(ClassCastException.class, () -> args.getKeyHashSlots(),
      "Expected ClassCastException when computing hash slots for RawString key");
  }

  /**
   * Test that using multiple Rawable keys throws ClassCastException when computing hash slots.
   */
  @Test
  public void testGetKeyHashSlots_withMultipleRawableKeys_throwsClassCastException() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);
    Rawable rawableKey1 = redis.clients.jedis.args.RawableFactory.from("{user}:1".getBytes());
    Rawable rawableKey2 = redis.clients.jedis.args.RawableFactory.from("{user}:2".getBytes());

    args.key(rawableKey1);
    args.key(rawableKey2);

    // This should throw ClassCastException
    assertThrows(ClassCastException.class, () -> args.getKeyHashSlots(),
      "Expected ClassCastException when computing hash slots for multiple Rawable keys");
  }

  /**
   * Test that using a mix of Rawable and String keys throws ClassCastException when computing hash
   * slots.
   */
  @Test
  public void testGetKeyHashSlots_withMixedRawableAndStringKeys_throwsClassCastException() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);

    // First add a String key - this works fine
    args.key("stringKey");

    // Then add a Rawable key
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("rawableKey".getBytes());
    args.key(rawableKey);

    // When iterating keys, the String key will be processed fine,
    // but the Rawable key will cause ClassCastException
    assertThrows(ClassCastException.class, () -> args.getKeyHashSlots(),
      "Expected ClassCastException when computing hash slots for mixed Rawable and String keys");
  }

  /**
   * Test that using a mix of Rawable and byte[] keys throws ClassCastException when computing hash
   * slots.
   */
  @Test
  public void testGetKeyHashSlots_withMixedRawableAndByteArrayKeys_throwsClassCastException() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);

    // First add a byte[] key - this works fine
    args.key("byteKey".getBytes());

    // Then add a Rawable key
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("rawableKey".getBytes());
    args.key(rawableKey);

    // When iterating keys, the byte[] key will be processed fine,
    // but the Rawable key will cause ClassCastException
    assertThrows(ClassCastException.class, () -> args.getKeyHashSlots(),
      "Expected ClassCastException when computing hash slots for mixed Rawable and byte[] keys");
  }

  /**
   * Verify that String keys work correctly (baseline test).
   */
  @Test
  public void testGetKeyHashSlots_withStringKey_worksCorrectly() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    String key = "testkey";

    args.key(key);

    // String keys should work correctly
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(1, slots.size());
    assertTrue(slots.contains(JedisClusterCRC16.getSlot(key)));
  }

  /**
   * Verify that byte[] keys work correctly (baseline test).
   */
  @Test
  public void testGetKeyHashSlots_withByteArrayKey_worksCorrectly() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    byte[] key = "testkey".getBytes();

    args.key(key);

    // byte[] keys should work correctly
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(1, slots.size());
    assertTrue(slots.contains(JedisClusterCRC16.getSlot(key)));
  }
}
