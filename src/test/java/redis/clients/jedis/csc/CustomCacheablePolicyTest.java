package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandMetadataResolverFactory;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;

/**
 * For now the custom policy decides alone, matching the pre-resolver behavior; the resolver gate
 * (custom may only narrow, never widen) is a planned follow-up — see the comment in
 * {@code CustomCacheablePolicy}.
 */
public class CustomCacheablePolicyTest {

  private static final Cacheable ALLOW_ALL = (command, keys) -> true;
  private static final Cacheable DENY_ALL = (command, keys) -> false;

  private static CacheabilityResolver resolver() {
    return new CacheabilityResolver(CommandMetadataResolverFactory.defaultResolver());
  }

  @Test
  public void customDecidesForEligibleCommands() {
    assertTrue(new CustomCacheablePolicy(ALLOW_ALL, resolver()).isCacheable(Command.GET,
      Collections.emptyList()));
    assertFalse(new CustomCacheablePolicy(DENY_ALL, resolver()).isCacheable(Command.GET,
      Collections.emptyList()));
  }

  /** Current behavior: the custom policy decides alone, even for metadata-ineligible commands. */
  @Test
  public void customDecidesAloneForNow() {
    CustomCacheablePolicy policy = new CustomCacheablePolicy(ALLOW_ALL, resolver());
    assertTrue(policy.isCacheable(Command.SET, Collections.emptyList()));
    assertTrue(policy.isCacheable(Command.XREAD, Collections.emptyList()));
    assertFalse(new CustomCacheablePolicy(DENY_ALL, resolver()).isCacheable(Command.GET,
      Collections.emptyList()));
  }

  @Test
  public void divergenceFromTheDefaultPolicyDoesNotChangeTheVerdict() {
    CustomCacheablePolicy policy = new CustomCacheablePolicy(ALLOW_ALL, resolver());
    assertTrue(policy.isCacheable(Command.SET, Collections.emptyList()));
    // and again, exercising the warn-once path
    assertTrue(policy.isCacheable(Command.SET, Collections.emptyList()));
    // through the CommandObject path, with a declared subcommand the default policy denies
    assertTrue(policy.isCacheable(
      new CommandObject<>(new CommandArguments(Command.XINFO, Keyword.CONSUMERS).key("k").add("g"),
          BuilderFactory.STRING)));
  }

  @Test
  public void customReceivesCommandAndKeys() {
    List<Object> keys = Arrays.asList("k1", "k2");
    Cacheable recording = (command, passedKeys) -> {
      assertEquals(Command.MGET, command);
      assertEquals(keys, passedKeys);
      return true;
    };
    assertTrue(new CustomCacheablePolicy(recording, resolver()).isCacheable(Command.MGET, keys));
  }

  @Test
  public void nullArgumentsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new CustomCacheablePolicy(null, resolver()));
    assertThrows(IllegalArgumentException.class, () -> new CustomCacheablePolicy(ALLOW_ALL, null));
  }
}
