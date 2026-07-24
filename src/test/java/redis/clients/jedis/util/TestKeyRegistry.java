package redis.clients.jedis.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.TestInfo;

import redis.clients.jedis.commands.KeyBinaryCommands;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Generates unique, human-readable Redis test keys and tracks them for cleanup.
 * <p>
 * Test endpoints are shared across the suite, so well-known keys (e.g. {@code "foo"}) may be left
 * behind by earlier tests holding an unexpected type. Keys produced here are namespaced by a test
 * identifier, and every generated key is registered so it can be deleted after the test.
 * <p>
 * The identifier — and thereby the registry's scope — depends on the lifecycle context the registry
 * is created in: {@code ClassName.methodName} in a method context
 * ({@code @BeforeEach}/{@code @Test}), {@code ClassName} in a class context ({@code @BeforeAll}). A
 * registry created in {@code @BeforeAll} and cleaned in {@code @AfterAll} holds class-level keys
 * that survive between individual tests; the two scopes cannot collide because method-scoped keys
 * always contain {@code .methodName}.
 * <p>
 * The {@code %test%} placeholder is replaced with the test identifier. Substitution is purely
 * textual — braces are never added implicitly, so to pin all keys of one test to the same Redis
 * Cluster slot, write the hash tag explicitly around the placeholder:
 *
 * <pre>
 * keys.key("user")          → StringCommandsTest.testSet:user
 * keys.key("%test%:user")   → StringCommandsTest.testSet:user
 * keys.key("{%test%}:user") → {StringCommandsTest.testSet}:user
 * </pre>
 *
 * Typical usage:
 *
 * <pre>
 * private TestKeyRegistry keys;
 *
 * &#64;BeforeEach
 * void setup(TestInfo testInfo) {
 *   keys = TestKeyRegistry.create(testInfo);
 * }
 *
 * &#64;AfterEach
 * void cleanup() {
 *   keys.cleanup(jedis);
 * }
 * </pre>
 */
public interface TestKeyRegistry {

  static TestKeyRegistry create(TestInfo testInfo) {
    return new Default(testInfo);
  }

  /**
   * Returns a registered key derived from the given pattern. A {@code %test%} placeholder is
   * replaced with the test identifier; a pattern without it is treated as a logical name and
   * prefixed with the test identifier.
   */
  String key(String pattern);

  /**
   * Binary counterpart of {@link #key(String)}: same pattern rules, returns the generated key
   * encoded with {@link SafeEncoder}. Names the same Redis key as {@code key(pattern)}.
   */
  byte[] bKey(String pattern);

  /** Registers an externally-created key for cleanup and returns it. Duplicates are ignored. */
  String register(String key);

  /**
   * Registers an externally-created binary key for cleanup and returns it. A copy is stored, so
   * later mutation of the array does not affect cleanup. Duplicates are ignored.
   */
  byte[] register(byte[] key);

  /**
   * Deletes all registered keys on each client (any Jedis client type), then clears the registry.
   */
  void cleanup(KeyBinaryCommands... clients);

  /**
   * Deletes all registered keys on each client (any Jedis client type), then clears the registry.
   */
  void cleanup(Iterable<? extends KeyBinaryCommands> clients);

  /** Clears the registry without deleting anything. */
  void reset();

  class Default implements TestKeyRegistry {

    private static final String TEST_PLACEHOLDER = "%test%";

    private final String testId;
    // Keys are stored as bytes so string- and binary-created keys share one registry entry
    // and cleanup covers both with a single binary DEL.
    private final Set<ByteBuffer> registeredKeys = ConcurrentHashMap.newKeySet();

    Default(TestInfo testInfo) {
      String className = testInfo.getTestClass().map(Class::getSimpleName).orElse("UnknownClass");
      // No test method in a class context (@BeforeAll) — the registry is class-scoped
      this.testId = testInfo.getTestMethod().map(m -> className + "." + m.getName())
          .orElse(className);
    }

    @Override
    public String key(String pattern) {
      return register(expand(pattern));
    }

    @Override
    public byte[] bKey(String pattern) {
      return register(SafeEncoder.encode(expand(pattern)));
    }

    @Override
    public String register(String key) {
      registeredKeys.add(ByteBuffer.wrap(SafeEncoder.encode(key)));
      return key;
    }

    @Override
    public byte[] register(byte[] key) {
      registeredKeys.add(ByteBuffer.wrap(Arrays.copyOf(key, key.length)));
      return key;
    }

    @Override
    public void cleanup(KeyBinaryCommands... clients) {
      cleanup(Arrays.asList(clients));
    }

    @Override
    public void cleanup(Iterable<? extends KeyBinaryCommands> clients) {
      if (registeredKeys.isEmpty()) {
        return;
      }
      List<byte[]> allKeys = new ArrayList<>(registeredKeys.size());
      for (ByteBuffer key : registeredKeys) {
        allKeys.add(key.array());
      }
      byte[][] keys = allKeys.toArray(new byte[0][]);
      for (KeyBinaryCommands client : clients) {
        delete(client, keys);
      }
      registeredKeys.clear();
    }

    @Override
    public void reset() {
      registeredKeys.clear();
    }

    private String expand(String pattern) {
      return pattern.contains(TEST_PLACEHOLDER) ? pattern.replace(TEST_PLACEHOLDER, testId)
          : testId + ":" + pattern;
    }

    private static void delete(KeyBinaryCommands client, byte[][] keys) {
      try {
        client.del(keys);
      } catch (JedisClusterOperationException e) {
        // legacy cluster clients reject cross-slot multi-key commands client-side
        deletePerSlot(client, keys);
      }
    }

    private static void deletePerSlot(KeyBinaryCommands client, byte[][] keys) {
      Map<Integer, List<byte[]>> keysBySlot = new HashMap<>();
      for (byte[] key : keys) {
        keysBySlot.computeIfAbsent(JedisClusterCRC16.getSlot(key), slot -> new ArrayList<>())
            .add(key);
      }
      for (List<byte[]> slotKeys : keysBySlot.values()) {
        client.del(slotKeys.toArray(new byte[0][]));
      }
    }
  }
}