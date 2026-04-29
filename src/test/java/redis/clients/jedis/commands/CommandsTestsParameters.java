package redis.clients.jedis.commands;

import java.util.Arrays;
import java.util.Collection;

import redis.clients.jedis.RedisProtocol;

public class CommandsTestsParameters {

  /**
   * RESP protocol versions we want our commands related tests to run against.
   * {@code null} means to use the default protocol which is assumed to be RESP2.
   */
  public static Collection<Object[]> respVersions() {
    return Arrays.asList(
        new Object[]{ RedisProtocol.RESP3_PREFERRED }, // Resolves to RESP3 on test env
        new Object[]{ RedisProtocol.RESP2 } // Both protocol=null and RESP2 are the same on test env
    );
  }

  /**
   * RESP protocol versions for tests that use the legacy {@link redis.clients.jedis.Jedis} client
   * (i.e. tests extending {@code JedisCommandsTestBase}).
   * {@code RESP3_PREFERRED} is excluded because {@link redis.clients.jedis.Jedis} does not support
   * auto-negotiation.
   */
  public static Collection<Object[]> jedisRespVersions() {
    return Arrays.asList(
        new Object[]{ null }, // Legacy Jedis doesn't explicitly send HELLO when protocol=null,
                              // so we need to test this case
        new Object[]{ RedisProtocol.RESP2 },
        new Object[]{ RedisProtocol.RESP3 }
    );
  }

}
