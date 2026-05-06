package redis.clients.jedis.commands;

import java.util.Arrays;
import java.util.Collection;

import redis.clients.jedis.RedisProtocol;

public class CommandsTestsParameters {

  /**
   * RESP protocol parametrization for {@link redis.clients.jedis.UnifiedJedis}-based tests. Two
   * cases are exercised:
   * <ul>
   *   <li>{@code null} – paired with the builder default {@code autoNegotiateProtocol=true},
   *       this is the alias for "RESP3 with graceful RESP2 fallback". On the test environment it
   *       resolves to RESP3 on the wire.</li>
   *   <li>{@link RedisProtocol#RESP2} – strict RESP2 negotiation.</li>
   * </ul>
   * Strict RESP3 is intentionally excluded — the auto-negotiation case already covers the RESP3
   * code path via the default config.
   */
  public static Collection<Object[]> respVersions() {
    return Arrays.asList(new Object[] { null }, // null + autoNegotiateProtocol=true (default) → RESP3 alias
        new Object[] { RedisProtocol.RESP2 });
  }

  /**
   * RESP protocol versions for tests that use the legacy {@link redis.clients.jedis.Jedis} client
   * (i.e. tests extending {@code JedisCommandsTestBase}). The {@code null} case here means
   * "legacy {@code HELLO}-less mode" — Jedis forces auto-negotiation off, so the connection
   * stays on RESP2 without sending {@code HELLO}.
   */
  public static Collection<Object[]> jedisRespVersions() {
    return Arrays.asList(new Object[] { null }, // Legacy Jedis doesn't explicitly send HELLO when protocol=null,
                                                // so we need to test this case
        new Object[] { RedisProtocol.RESP2 }, new Object[] { RedisProtocol.RESP3 });
  }

}
