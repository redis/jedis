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

  @Test
  public void testGetKeyHashSlots_withRawableFromByteArray() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("testkey".getBytes());

    // Using key() with a Rawable should store the Rawable object in the keys list
    args.key(rawableKey);

    // This should not throw an exception
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(1, slots.size());
    assertTrue(slots.contains(JedisClusterCRC16.getSlot("testkey".getBytes())));
  }

  @Test
  public void testGetKeyHashSlots_withRawableFromString() {
    CommandArguments args = new CommandArguments(Protocol.Command.GET);
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("testkey");

    args.key(rawableKey);

    // This should not throw an exception
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(1, slots.size());
    assertTrue(slots.contains(JedisClusterCRC16.getSlot("testkey")));
  }

  @Test
  public void testGetKeyHashSlots_withMultipleRawableKeys() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);
    Rawable rawableKey1 = redis.clients.jedis.args.RawableFactory.from("{user}:1".getBytes());
    Rawable rawableKey2 = redis.clients.jedis.args.RawableFactory.from("{user}:2".getBytes());

    args.key(rawableKey1);
    args.key(rawableKey2);

    // This should not throw an exception
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(1, slots.size());
    assertTrue(slots.contains(JedisClusterCRC16.getSlot("{user}:1")));
  }

  @Test
  public void testGetKeyHashSlots_withMixedRawableAndStringKeys() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);

    // First add a String key - this works fine
    args.key("stringKey");

    // Then add a Rawable key
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("rawableKey".getBytes());
    args.key(rawableKey);

    // This should not throw an exception
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(2, slots.size());
  }

  /**
   * Test that using a mix of Rawable and byte[] keys throws ClassCastException when computing hash
   * slots.
   */
  @Test
  public void testGetKeyHashSlots_withMixedRawableAndByteArrayKeys() {
    CommandArguments args = new CommandArguments(Protocol.Command.MGET);

    // First add a byte[] key - this works fine
    args.key("byteKey".getBytes());

    // Then add a Rawable key
    Rawable rawableKey = redis.clients.jedis.args.RawableFactory.from("rawableKey".getBytes());
    args.key(rawableKey);

    // This should not throw an exception
    java.util.Set<Integer> slots = args.getKeyHashSlots();
    assertEquals(2, slots.size());
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

  // ==================== Key Preprocessor Hash Slot Tests ====================
  // These tests verify that the keyPreProcessor is properly accounted for when
  // calculating hash slots for multi-shard grouping.

  /**
   * Test that demonstrates the bug where groupArgumentsByKeyHashSlot calculates hash slots from the
   * original keys but the keyPreProcessor transforms them into different keys with different hash
   * slots. Bug: When a prefix like "{prefix}:" is added by keyPreProcessor, the original keys
   * "key1" and "key2" might hash to different slots, but after prefixing they become
   * "{prefix}:key1" and "{prefix}:key2" which hash to the same slot (due to the hash tag). The slot
   * calculation should use the preprocessed keys.
   */
  @Test
  public void testGroupArgumentsByKeyHashSlot_withKeyPreProcessor_usesPreprocessedSlot() {
    // Create ClusterCommandObjects with a key preprocessor that adds a hash tag prefix
    ClusterCommandObjects clusterCmdObjects = new ClusterCommandObjects();
    // The prefix "{sameSlot}:" ensures all keys hash to the same slot
    clusterCmdObjects.setKeyArgumentPreProcessor(
      new redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor("{sameSlot}:"));

    // These keys hash to different slots without the prefix
    String[] keys = { "key1", "key2", "key3" };
    int slot1 = JedisClusterCRC16.getSlot("key1");
    int slot2 = JedisClusterCRC16.getSlot("key2");
    int slot3 = JedisClusterCRC16.getSlot("key3");
    // Verify the original keys hash to at least 2 different slots
    assertTrue(slot1 != slot2 || slot2 != slot3 || slot1 != slot3,
      "Test setup error: original keys should hash to different slots");

    // After preprocessing, all keys should hash to the same slot because of the hash tag
    int expectedSlot = JedisClusterCRC16.getSlot("{sameSlot}:key1");
    assertEquals(expectedSlot, JedisClusterCRC16.getSlot("{sameSlot}:key2"),
      "Preprocessed keys should hash to the same slot due to hash tag");
    assertEquals(expectedSlot, JedisClusterCRC16.getSlot("{sameSlot}:key3"),
      "Preprocessed keys should hash to the same slot due to hash tag");

    // Call groupArgumentsByKeyHashSlot
    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCmdObjects.groupArgumentsByKeyHashSlot(args, keys, null);

    // BUG REPRODUCTION: With the bug, this would return multiple CommandArguments
    // because the slot calculation uses the original keys which hash to different slots.
    // The fix should make this return 1 CommandArguments because after preprocessing,
    // all keys hash to the same slot (due to the "{sameSlot}:" hash tag prefix).
    assertEquals(1, result.size(),
      "After preprocessing with hash tag prefix, all keys should group into one slot. " + "Got "
          + result.size()
          + " groups instead of 1, indicating slot calculation used original keys.");

    // Verify the preprocessed keys are in the result
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{sameSlot}:key1"));
    assertTrue(argsStrings.contains("{sameSlot}:key2"));
    assertTrue(argsStrings.contains("{sameSlot}:key3"));
  }

  /**
   * Test that groupArgumentsByKeyValueHashSlot also accounts for keyPreProcessor when calculating
   * hash slots for key-value pairs.
   */
  @Test
  public void testGroupArgumentsByKeyValueHashSlot_withKeyPreProcessor_usesPreprocessedSlot() {
    ClusterCommandObjects clusterCmdObjects = new ClusterCommandObjects();
    clusterCmdObjects.setKeyArgumentPreProcessor(
      new redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor("{sameSlot}:"));

    // These key-value pairs have keys that hash to different slots without the prefix
    String[] keysValues = { "key1", "value1", "key2", "value2", "key3", "value3" };

    // Verify original keys hash to different slots
    int slot1 = JedisClusterCRC16.getSlot("key1");
    int slot2 = JedisClusterCRC16.getSlot("key2");
    int slot3 = JedisClusterCRC16.getSlot("key3");
    assertTrue(slot1 != slot2 || slot2 != slot3 || slot1 != slot3,
      "Test setup error: original keys should hash to different slots");

    CommandArguments args = new CommandArguments(Protocol.Command.MSET);
    List<CommandArguments> result = clusterCmdObjects.groupArgumentsByKeyValueHashSlot(args,
      keysValues, null);

    // With the fix, all keys should group into one slot
    assertEquals(1, result.size(),
      "After preprocessing with hash tag prefix, all keys should group into one slot");

    // Verify the preprocessed keys and values are in the result
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{sameSlot}:key1"));
    assertTrue(argsStrings.contains("value1"));
    assertTrue(argsStrings.contains("{sameSlot}:key2"));
    assertTrue(argsStrings.contains("value2"));
    assertTrue(argsStrings.contains("{sameSlot}:key3"));
    assertTrue(argsStrings.contains("value3"));
  }

  /**
   * Test the reverse case: keys that originally hash to the same slot but after preprocessing hash
   * to different slots.
   */
  @Test
  public void testGroupArgumentsByKeyHashSlot_withKeyPreProcessor_differentSlotsAfterPreprocess() {
    ClusterCommandObjects clusterCmdObjects = new ClusterCommandObjects();
    // This preprocessor changes the hash tag, causing keys to hash to different slots
    clusterCmdObjects.setKeyArgumentPreProcessor(key -> {
      String keyStr = (String) key;
      // Remove the hash tag, making keys hash based on their full content
      return keyStr.replace("{same}:", "different:");
    });

    // These keys all hash to the same slot due to the {same} hash tag
    String[] keys = { "{same}:key1", "{same}:key2", "{same}:key3" };
    int originalSlot = JedisClusterCRC16.getSlot("{same}:key1");
    assertEquals(originalSlot, JedisClusterCRC16.getSlot("{same}:key2"));
    assertEquals(originalSlot, JedisClusterCRC16.getSlot("{same}:key3"));

    // After preprocessing, keys become "different:key1", etc. which hash to different slots
    int slot1 = JedisClusterCRC16.getSlot("different:key1");
    int slot2 = JedisClusterCRC16.getSlot("different:key2");
    int slot3 = JedisClusterCRC16.getSlot("different:key3");
    // At least some should be different (depending on actual hash values)
    assertTrue(slot1 != slot2 || slot2 != slot3 || slot1 != slot3,
      "Test setup: preprocessed keys should hash to different slots");

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCmdObjects.groupArgumentsByKeyHashSlot(args, keys, null);

    // With the fix, keys should be grouped by their preprocessed slot values
    // which should result in multiple groups (not just 1 as with original keys)
    assertTrue(result.size() > 1,
      "After preprocessing removes hash tag, keys should be in different slots. " + "Got "
          + result.size() + " groups.");
  }

  /**
   * Test with byte[] keys and keyPreProcessor.
   */
  @Test
  public void testGroupArgumentsByKeyHashSlot_withKeyPreProcessor_binaryKeys() {
    ClusterCommandObjects clusterCmdObjects = new ClusterCommandObjects();
    clusterCmdObjects.setKeyArgumentPreProcessor(
      new redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor("{sameSlot}:"));

    byte[][] keys = { "key1".getBytes(), "key2".getBytes(), "key3".getBytes() };

    CommandArguments args = new CommandArguments(Protocol.Command.DEL);
    List<CommandArguments> result = clusterCmdObjects.groupArgumentsByKeyHashSlot(args, keys, null);

    // With the fix, all keys should group into one slot due to the hash tag prefix
    assertEquals(1, result.size(),
      "After preprocessing with hash tag prefix, all binary keys should group into one slot");

    // Verify the preprocessed keys are in the result
    List<String> argsStrings = extractArgsAsStrings(result.get(0));
    assertTrue(argsStrings.contains("{sameSlot}:key1"));
    assertTrue(argsStrings.contains("{sameSlot}:key2"));
    assertTrue(argsStrings.contains("{sameSlot}:key3"));
  }
}
