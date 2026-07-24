package redis.clients.jedis.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.TestInfo;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Generates unique, human-readable Redis test keys and tracks them for cleanup.
 * <p>
 * Test endpoints are shared across the suite, so well-known keys (e.g. {@code "foo"}) may be left
 * behind by earlier tests holding an unexpected type. Keys produced here are namespaced by the
 * test identifier {@code ClassName.methodName}, and every generated key is registered so it can
 * be deleted after the test.
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

  /** Registers an externally-created key for cleanup and returns it. Duplicates are ignored. */
  String register(String key);

  /** Deletes all registered keys on each client, then clears the registry. */
  void cleanup(UnifiedJedis... clients);

  /** Deletes all registered keys on each client, then clears the registry. */
  void cleanup(Iterable<? extends UnifiedJedis> clients);

  /** Clears the registry without deleting anything. */
  void reset();

  class Default implements TestKeyRegistry {

    private static final String TEST_PLACEHOLDER = "%test%";

    private final String testId;
    private final Set<String> registeredKeys = ConcurrentHashMap.newKeySet();

    Default(TestInfo testInfo) {
      String className = testInfo.getTestClass().map(Class::getSimpleName).orElse("UnknownClass");
      String methodName = testInfo.getTestMethod().map(Method::getName).orElse("unknownMethod");
      this.testId = className + "." + methodName;
    }

    @Override
    public String key(String pattern) {
      String key = pattern.contains(TEST_PLACEHOLDER)
          ? pattern.replace(TEST_PLACEHOLDER, testId)
          : testId + ":" + pattern;
      return register(key);
    }

    @Override
    public String register(String key) {
      registeredKeys.add(key);
      return key;
    }

    @Override
    public void cleanup(UnifiedJedis... clients) {
      cleanup(Arrays.asList(clients));
    }

    @Override
    public void cleanup(Iterable<? extends UnifiedJedis> clients) {
      if (registeredKeys.isEmpty()) {
        return;
      }
      String[] keys = registeredKeys.toArray(new String[0]);
      for (UnifiedJedis client : clients) {
        delete(client, keys);
      }
      registeredKeys.clear();
    }

    @Override
    public void reset() {
      registeredKeys.clear();
    }

    private static void delete(UnifiedJedis client, String[] keys) {
      try {
        client.del(keys);
      } catch (JedisDataException e) {
        if (!isCrossSlotError(e)) {
          throw e;
        }
        deletePerSlot(client, keys);
      }
    }

    private static boolean isCrossSlotError(JedisDataException e) {
      return e.getMessage() != null && e.getMessage().startsWith("CROSSSLOT");
    }

    private static void deletePerSlot(UnifiedJedis client, String[] keys) {
      Map<Integer, List<String>> keysBySlot = new HashMap<>();
      for (String key : keys) {
        keysBySlot.computeIfAbsent(JedisClusterCRC16.getSlot(key), slot -> new ArrayList<>())
            .add(key);
      }
      for (List<String> slotKeys : keysBySlot.values()) {
        client.del(slotKeys.toArray(new String[0]));
      }
    }
  }
}