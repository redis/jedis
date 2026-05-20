package redis.clients.jedis.commands.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisProtocol;

/**
 * Tests {@code CONFIG GET} and {@code CONFIG SET} via {@link redis.clients.jedis.UnifiedJedis}
 * against a real Redis deployment. Subclasses parameterize by {@link RedisProtocol} so the same
 * assertions cover both RESP2 and RESP3, and by deployment topology (standalone vs OSS Cluster) so
 * the request/response policy wiring is exercised end-to-end.
 * <p>
 * Parameter names used here ({@code slowlog-max-len} and {@code slowlog-log-slower-than}) are
 * supported by both Redis OSS and Redis Enterprise per the
 * <a href="https://redis.io/docs/latest/operate/rs/references/compatibility/config-settings/">
 * Redis Enterprise config compatibility table</a>, so the same suite is portable across
 * deployments.
 */
@Tag("integration")
public abstract class ConfigCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String PARAM_SLOWLOG_MAX_LEN = "slowlog-max-len";
  protected static final String PARAM_SLOWLOG_LOG_SLOWER_THAN = "slowlog-log-slower-than";

  public ConfigCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void configGetSinglePattern() {
    Map<String, String> reply = jedis.configGet(PARAM_SLOWLOG_MAX_LEN);

    assertThat(reply, aMapWithSize(1));
    assertThat(reply, hasKey(PARAM_SLOWLOG_MAX_LEN));
  }

  @Test
  public void configGetMultiplePatterns() {
    Map<String, String> reply = jedis.configGet(PARAM_SLOWLOG_MAX_LEN,
      PARAM_SLOWLOG_LOG_SLOWER_THAN);

    assertThat(reply, aMapWithSize(2));
    assertThat(reply, hasKey(PARAM_SLOWLOG_MAX_LEN));
    assertThat(reply, hasKey(PARAM_SLOWLOG_LOG_SLOWER_THAN));
  }

  @Test
  public void configGetWildcardReturnsManyEntries() {
    Map<String, String> reply = jedis.configGet("*");

    assertThat(reply.size(), greaterThanOrEqualTo(1));
  }

  @Test
  public void configGetUnknownParameterReturnsEmpty() {
    Map<String, String> reply = jedis.configGet("definitely-not-a-real-config-name-xyz");

    assertThat(reply, equalTo(java.util.Collections.<String, String> emptyMap()));
  }

  @Test
  public void configSetSingleParameter() {
    String original = jedis.configGet(PARAM_SLOWLOG_MAX_LEN).get(PARAM_SLOWLOG_MAX_LEN);
    try {
      assertThat(jedis.configSet(PARAM_SLOWLOG_MAX_LEN, "200"), equalTo("OK"));
      assertThat(jedis.configGet(PARAM_SLOWLOG_MAX_LEN).get(PARAM_SLOWLOG_MAX_LEN), equalTo("200"));
    } finally {
      jedis.configSet(PARAM_SLOWLOG_MAX_LEN, original);
    }
  }

  @Test
  public void configSetMultipleParameters() {
    Map<String, String> originals = jedis.configGet(PARAM_SLOWLOG_MAX_LEN,
      PARAM_SLOWLOG_LOG_SLOWER_THAN);
    Map<String, String> updates = new HashMap<>();
    updates.put(PARAM_SLOWLOG_MAX_LEN, "200");
    updates.put(PARAM_SLOWLOG_LOG_SLOWER_THAN, "20000");
    try {
      assertThat(jedis.configSet(updates), equalTo("OK"));

      Map<String, String> reply = jedis.configGet(PARAM_SLOWLOG_MAX_LEN,
        PARAM_SLOWLOG_LOG_SLOWER_THAN);
      assertThat(reply, aMapWithSize(2));
      assertThat(reply.get(PARAM_SLOWLOG_MAX_LEN), equalTo("200"));
      assertThat(reply.get(PARAM_SLOWLOG_LOG_SLOWER_THAN), equalTo("20000"));
    } finally {
      jedis.configSet(originals);
    }
  }
}
