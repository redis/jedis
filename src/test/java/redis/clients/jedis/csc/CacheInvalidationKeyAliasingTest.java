package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.util.SafeEncoder;

public class CacheInvalidationKeyAliasingTest {

  @Test
  public void serverInvalidationEvictsEntryCachedWithAReusedKeyBuffer() {
    byte[] key = SafeEncoder.encode("foo");
    CacheKey<String> cacheKey = new CacheKey<>(
        new CommandObject<>(new CommandArguments(Command.GET).key(key), BuilderFactory.STRING));

    Cache cache = new TestCache();
    cache.set(cacheKey, new CacheEntry<>(cacheKey, "cached", null));
    assertTrue(cache.hasCacheKey(cacheKey));

    key[0] = 'b'; // caller reuses its buffer for the next command

    cache.deleteByRedisKeys(Collections.singletonList(SafeEncoder.encode("foo")));

    assertFalse(cache.hasCacheKey(cacheKey));
  }
}
