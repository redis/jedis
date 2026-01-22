package redis.clients.jedis;

import redis.clients.jedis.params.IParams;
import redis.clients.jedis.params.MSetExParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.JedisClusterHashTag;
import redis.clients.jedis.util.KeyValue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.TYPE;

public class ClusterCommandObjects extends CommandObjects {

  private static final String CLUSTER_UNSUPPORTED_MESSAGE = "Not supported in cluster mode.";

  private static final String KEYS_PATTERN_MESSAGE = "Cluster mode only supports KEYS command"
      + " with pattern containing hash-tag ( curly-brackets enclosed string )";

  private static final String SCAN_PATTERN_MESSAGE = "Cluster mode only supports SCAN command"
      + " with MATCH pattern containing hash-tag ( curly-brackets enclosed string )";

  @Override
  public final CommandObject<Set<String>> keys(String pattern) {
    if (!JedisClusterHashTag.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(KEYS_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(KEYS).key(pattern), BuilderFactory.STRING_SET);
  }

  @Override
  public final CommandObject<Set<byte[]>> keys(byte[] pattern) {
    if (!JedisClusterHashTag.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(KEYS_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(KEYS).key(pattern), BuilderFactory.BINARY_SET);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor) {
    throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params) {
    String match = params.match();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).addHashSlotKey(match), BuilderFactory.SCAN_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    String match = params.match();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).addHashSlotKey(match).add(TYPE).add(type), BuilderFactory.SCAN_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor) {
    throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params) {
    byte[] match = params.binaryMatch();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).addHashSlotKey(match), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type) {
    byte[] match = params.binaryMatch();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).addHashSlotKey(match).add(TYPE).add(type), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<Long> waitReplicas(int replicas, long timeout) {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

  @Override
  public CommandObject<KeyValue<Long, Long>> waitAOF(long numLocal, long numReplicas, long timeout) {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

  /**
   * Groups key-value pairs by their hash slot and creates separate CommandArguments for each slot.
   * This enables commands with multiple keys to be properly distributed across Redis cluster nodes
   * by ensuring that each resulting command only operates on keys that hash to the same slot.
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keysValues variable number of key-value pairs to be grouped (must be even length)
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys/values that belong to the same hash slot
   * @throws IllegalArgumentException if keysValues has odd length
   */
  protected List<CommandArguments> groupArgumentsByKeyValueHashSlot(CommandArguments args, String[] keysValues, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keysValues,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        CommandArguments::add,
        false
    );
  }

  /**
   * Groups key-value pairs by their hash slot and creates separate CommandArguments for each slot.
   * This enables commands with multiple keys to be properly distributed across Redis cluster nodes
   * by ensuring that each resulting command only operates on keys that hash to the same slot.
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keysValues variable number of key-value pairs to be grouped (must be even length)
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys/values that belong to the same hash slot
   * @throws IllegalArgumentException if keysValues has odd length
   */
  protected List<CommandArguments> groupArgumentsByKeyValueHashSlot(CommandArguments args, byte[][] keysValues, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keysValues,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        CommandArguments::add,
        false
    );
  }

  /**
   * Groups keys by their hash slot and creates separate CommandArguments for each slot.
   * This enables commands with multiple keys (like DEL, EXISTS, MGET) to be properly distributed
   * across Redis cluster nodes by ensuring that each resulting command only operates on keys
   * that hash to the same slot.
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keys variable number of keys to be grouped
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys that belong to the same hash slot
   */
  protected List<CommandArguments> groupArgumentsByKeyHashSlot(CommandArguments args, String[] keys, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keys,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        null,
        false
    );
  }

  /**
   * Groups keys by their hash slot and creates separate CommandArguments for each slot.
   * This enables commands with multiple keys (like DEL, EXISTS, MGET) to be properly distributed
   * across Redis cluster nodes by ensuring that each resulting command only operates on keys
   * that hash to the same slot.
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keys variable number of keys to be grouped
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys that belong to the same hash slot
   */
  protected List<CommandArguments> groupArgumentsByKeyHashSlot(CommandArguments args, byte[][] keys, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keys,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        null,
        false
    );
  }

  /**
   * Groups key-value pairs by their hash slot and creates separate CommandArguments for each slot.
   * Inserts the key count after the command but before the keys (for commands like MSETEX).
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keysValues variable number of key-value pairs to be grouped (must be even length)
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys/values that belong to the same hash slot
   * @throws IllegalArgumentException if keysValues has odd length
   */
  protected List<CommandArguments> groupArgumentsByKeyValueHashSlotWithKeyCount(CommandArguments args, String[] keysValues, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keysValues,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        CommandArguments::add,
        true
    );
  }

  /**
   * Groups key-value pairs by their hash slot and creates separate CommandArguments for each slot.
   * Inserts the key count after the command but before the keys (for commands like MSETEX).
   *
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keysValues variable number of key-value pairs to be grouped (must be even length)
   * @param params additional parameters for the command (may be null)
   * @return a list of CommandArguments objects, each containing only keys/values that belong to the same hash slot
   * @throws IllegalArgumentException if keysValues has odd length
   */
  protected List<CommandArguments> groupArgumentsByKeyValueHashSlotWithKeyCount(CommandArguments args, byte[][] keysValues, IParams params) {
    return groupArgumentsByKeyValueHashSlotImpl(
        args,
        keysValues,
        params,
        JedisClusterCRC16::getSlot,
        CommandArguments::key,
        CommandArguments::add,
        true
    );
  }

  /**
   * Internal helper method that implements the common logic for grouping keys (and optionally values) by hash slot.
   * When valueAdder is null, this method processes keys only (for commands like DEL, EXISTS, MGET).
   * When valueAdder is provided, this method processes key-value pairs (for commands like MSET).
   *
   * @param <T> the type of key/value elements (String or byte[])
   * @param args the original command arguments to copy (the command itself will be preserved)
   * @param keysOrKeysValues array of keys (when valueAdder is null) or key-value pairs (when valueAdder is provided)
   * @param params additional parameters for the command (may be null)
   * @param slotCalculator function to calculate the hash slot for a key
   * @param keyAdder function to add a key to CommandArguments
   * @param valueAdder function to add a value to CommandArguments (may be null for key-only operations)
   * @param insertKeyCount if true, inserts the number of keys after the command but before the keys (for commands like MSETEX)
   * @return a list of CommandArguments objects, each containing only keys/values that belong to the same hash slot
   * @throws IllegalArgumentException if valueAdder is provided and keysOrKeysValues has odd length
   */
  private <T> List<CommandArguments> groupArgumentsByKeyValueHashSlotImpl(
      CommandArguments args,
      T[] keysOrKeysValues,
      IParams params,
      Function<T, Integer> slotCalculator,
      BiConsumer<CommandArguments, T> keyAdder,
      BiConsumer<CommandArguments, T> valueAdder,
      boolean insertKeyCount) {

    boolean keyValueMode = valueAdder != null;
    int step = keyValueMode ? 2 : 1;

    if (keyValueMode && keysOrKeysValues.length % 2 != 0) {
      throw new IllegalArgumentException("keysValues must contain an even number of elements (key-value pairs)");
    }

    // Group keys (and optionally values) by hash slot
    Map<Integer, List<T>> slotToElements = new HashMap<>();
    for (int i = 0; i < keysOrKeysValues.length; i += step) {
      T key = keysOrKeysValues[i];
      int slot = slotCalculator.apply(key);

      slotToElements.computeIfAbsent(slot, k -> new ArrayList<>()).add(key);
      if (keyValueMode) {
        slotToElements.get(slot).add(keysOrKeysValues[i + 1]);
      }
    }

    // Create CommandArguments for each hash slot group
    List<CommandArguments> result = new ArrayList<>();
    for (List<T> groupedElements : slotToElements.values()) {
      CommandArguments slotArgs = commandArguments(args.getCommand());

      // Insert key count after command but before keys (e.g., numkeys for MSETEX)
      if (insertKeyCount) {
        int keyCount = groupedElements.size() / step;
        slotArgs.add(keyCount);
      }

      // Add keys (and optionally values) for this slot
      for (int i = 0; i < groupedElements.size(); i += step) {
        keyAdder.accept(slotArgs, groupedElements.get(i));
        if (keyValueMode) {
          valueAdder.accept(slotArgs, groupedElements.get(i + 1));
        }
      }

      // Add params if provided
      if (params != null) {
        slotArgs.addParams(params);
      }

      result.add(slotArgs);
    }

    return result;
  }

  // ==================== Multi-Shard Command Methods ====================
  // These methods split commands across multiple Redis cluster shards based on key hash slots.
  // They return List<CommandObject<T>> where each CommandObject targets keys in the same hash slot.

  /**
   * Creates multiple DEL command objects, one for each hash slot group.
   * This enables the DEL command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to delete
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> delMultiShard(String... keys) {
    CommandArguments args = commandArguments(DEL);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple DEL command objects, one for each hash slot group.
   * This enables the DEL command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to delete
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> delMultiShard(byte[]... keys) {
    CommandArguments args = commandArguments(DEL);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple EXISTS command objects, one for each hash slot group.
   * This enables the EXISTS command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to check for existence
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> existsMultiShard(String... keys) {
    CommandArguments args = commandArguments(EXISTS);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple EXISTS command objects, one for each hash slot group.
   * This enables the EXISTS command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to check for existence
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> existsMultiShard(byte[]... keys) {
    CommandArguments args = commandArguments(EXISTS);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MGET command objects, one for each hash slot group.
   * This enables the MGET command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to retrieve values for
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<List<String>>> mgetMultiShard(String... keys) {
    CommandArguments args = commandArguments(MGET);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.STRING_LIST))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MGET command objects, one for each hash slot group.
   * This enables the MGET command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to retrieve values for
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<List<byte[]>>> mgetMultiShard(byte[]... keys) {
    CommandArguments args = commandArguments(MGET);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.BINARY_LIST))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MSET command objects, one for each hash slot group.
   * This enables the MSET command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keysvalues alternating keys and values (key1, value1, key2, value2, ...)
   * @return a list of CommandObject instances, each containing key-value pairs that belong to the same hash slot
   */
  public List<CommandObject<String>> msetMultiShard(String... keysvalues) {
    CommandArguments args = commandArguments(MSET);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyValueHashSlot(args, keysvalues, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.STRING))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MSET command objects, one for each hash slot group.
   * This enables the MSET command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keysvalues alternating keys and values (key1, value1, key2, value2, ...)
   * @return a list of CommandObject instances, each containing key-value pairs that belong to the same hash slot
   */
  public List<CommandObject<String>> msetMultiShard(byte[]... keysvalues) {
    CommandArguments args = commandArguments(MSET);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyValueHashSlot(args, keysvalues, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.STRING))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple TOUCH command objects, one for each hash slot group.
   * This enables the TOUCH command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to touch
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> touchMultiShard(String... keys) {
    CommandArguments args = commandArguments(TOUCH);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple TOUCH command objects, one for each hash slot group.
   * This enables the TOUCH command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to touch
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> touchMultiShard(byte[]... keys) {
    CommandArguments args = commandArguments(TOUCH);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple UNLINK command objects, one for each hash slot group.
   * This enables the UNLINK command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to unlink
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> unlinkMultiShard(String... keys) {
    CommandArguments args = commandArguments(UNLINK);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple UNLINK command objects, one for each hash slot group.
   * This enables the UNLINK command to be executed across multiple Redis cluster shards
   * when keys hash to different slots.
   *
   * @param keys the keys to unlink
   * @return a list of CommandObject instances, each containing keys that belong to the same hash slot
   */
  public List<CommandObject<Long>> unlinkMultiShard(byte[]... keys) {
    CommandArguments args = commandArguments(UNLINK);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyHashSlot(args, keys, null);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.LONG))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MSETEX command objects, one for each hash slot group.
   * This enables the MSETEX command to be executed across multiple Redis cluster shards
   * when keys hash to different slots. Each command preserves the provided parameters
   * (expiration, NX/XX conditions).
   *
   * @param params the MSETEX parameters (expiration, NX/XX conditions)
   * @param keysvalues alternating keys and values (key1, value1, key2, value2, ...)
   * @return a list of CommandObject instances, each containing key-value pairs that belong to the same hash slot
   */
  public List<CommandObject<Boolean>> msetexMultiShard(MSetExParams params, String... keysvalues) {
    CommandArguments args = commandArguments(MSETEX);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyValueHashSlotWithKeyCount(args, keysvalues, params);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.BOOLEAN))
        .collect(Collectors.toList());
  }

  /**
   * Creates multiple MSETEX command objects, one for each hash slot group.
   * This enables the MSETEX command to be executed across multiple Redis cluster shards
   * when keys hash to different slots. Each command preserves the provided parameters
   * (expiration, NX/XX conditions).
   *
   * @param params the MSETEX parameters (expiration, NX/XX conditions)
   * @param keysvalues alternating keys and values (key1, value1, key2, value2, ...)
   * @return a list of CommandObject instances, each containing key-value pairs that belong to the same hash slot
   */
  public List<CommandObject<Boolean>> msetexMultiShard(MSetExParams params, byte[]... keysvalues) {
    CommandArguments args = commandArguments(MSETEX);
    List<CommandArguments> groupedArgs = groupArgumentsByKeyValueHashSlotWithKeyCount(args, keysvalues, params);
    return groupedArgs.stream()
        .map(cmdArgs -> new CommandObject<>(cmdArgs, BuilderFactory.BOOLEAN))
        .collect(Collectors.toList());
  }

}
