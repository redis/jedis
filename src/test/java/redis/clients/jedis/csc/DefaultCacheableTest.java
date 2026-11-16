package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Protocol;

public class DefaultCacheableTest {

  /** BITRANGEINTARRAY is cacheable by default. */
  @Test
  public void bitrangeintarrayIsDefaultCacheable() {
    assertTrue(DefaultCacheable.isDefaultCacheableCommand(Protocol.Command.BITRANGEINTARRAY));
  }
}
