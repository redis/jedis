package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Integration tests for the direct {@code HIMPORT} commands on the single-connection legacy
 * {@link redis.clients.jedis.Jedis} client. Affinity is guaranteed by the owned connection, so no
 * session handle is needed. Gated on server support for {@code HIMPORT} (Redis 8.10+).
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#jedisRespVersions")
@Tag("integration")
@EnabledOnCommand("HIMPORT")
public class HashImportCommandsIT extends JedisCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(() -> endpoint);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> endpoint);

  public HashImportCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void prepareSetAndReadBack() {
    assertEquals("OK", jedis.himportPrepare("u", "name", "email", "age"));
    assertEquals("OK", jedis.himportSet("user:1", "u", "alice", "alice@example.com", "30"));
    assertEquals("OK", jedis.himportSet("user:2", "u", "bob", "bob@example.com", "25"));

    Map<String, String> expected = new HashMap<>();
    expected.put("name", "alice");
    expected.put("email", "alice@example.com");
    expected.put("age", "30");
    assertEquals(expected, jedis.hgetAll("user:1"));
    assertEquals("bob", jedis.hget("user:2", "name"));
  }

  @Test
  public void discardAndDiscardAll() {
    jedis.himportPrepare("a", "f1");
    jedis.himportPrepare("b", "f1");
    assertEquals(1L, jedis.himportDiscard("a"));
    assertEquals(0L, jedis.himportDiscard("a"));
    assertEquals(1L, jedis.himportDiscardAll());
    assertEquals(0L, jedis.himportDiscardAll());
  }

  @Test
  public void serverErrorsPropagate() {
    JedisDataException noSuch = assertThrows(JedisDataException.class,
      () -> jedis.himportSet("k", "missing", "a"));
    assertThat(noSuch.getMessage(), equalTo("ERR no such fieldset"));

    JedisDataException dup = assertThrows(JedisDataException.class,
      () -> jedis.himportPrepare("dup", "f1", "f1"));
    assertThat(dup.getMessage(), equalTo("ERR duplicate field name in fieldset"));
  }

  @Test
  public void binaryPrepareSetAndReadBack() {
    byte[] key = SafeEncoder.encode("buser:1");
    byte[] fs = SafeEncoder.encode("bu");
    byte[] fName = SafeEncoder.encode("name");
    byte[] fAge = SafeEncoder.encode("age");
    byte[] vName = SafeEncoder.encode("carol");
    byte[] vAge = SafeEncoder.encode("41");

    assertEquals("OK", jedis.himportPrepare(fs, fName, fAge));
    assertEquals("OK", jedis.himportSet(key, fs, vName, vAge));
    assertEquals(1L, jedis.himportDiscard(fs));

    assertArrayEquals(vName, jedis.hget(key, fName));
  }
}
