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
        new Object[]{ null },
        new Object[]{ RedisProtocol.RESP2 },
        new Object[]{ RedisProtocol.RESP3 }
    );
  }

}
