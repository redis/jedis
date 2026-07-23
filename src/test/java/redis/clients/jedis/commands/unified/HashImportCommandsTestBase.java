package redis.clients.jedis.commands.unified;

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

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.AbstractHashImportHandler;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Integration tests for the {@code HIMPORT} (Hinted Hash Templates) session handle obtained from
 * {@link redis.clients.jedis.UnifiedJedis#hashImport()}. Gated on server support for
 * {@code HIMPORT} (Redis 8.10+).
 */
@Tag("integration")
@EnabledOnCommand("HIMPORT")
public abstract class HashImportCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public HashImportCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void prepareSetAndReadBack() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      assertEquals("OK", hi.himportPrepare("u", "name", "email", "age"));
      assertEquals("OK", hi.himportSet("user:1", "u", "alice", "alice@example.com", "30"));
      assertEquals("OK", hi.himportSet("user:2", "u", "bob", "bob@example.com", "25"));
    }

    // Keys created via HIMPORT SET are ordinary hashes, readable by any connection.
    Map<String, String> expected1 = new HashMap<>();
    expected1.put("name", "alice");
    expected1.put("email", "alice@example.com");
    expected1.put("age", "30");
    assertEquals(expected1, jedis.hgetAll("user:1"));
    assertEquals("bob", jedis.hget("user:2", "name"));
  }

  @Test
  public void discardReturnsValue() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      hi.himportPrepare("u", "f1", "f2");
      assertEquals(1L, hi.himportDiscard("u")); // removed
      assertEquals(0L, hi.himportDiscard("u")); // already gone
    }
  }

  @Test
  public void discardAllCountsFieldsets() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      hi.himportPrepare("a", "f1");
      hi.himportPrepare("b", "f1");
      assertEquals(2L, hi.himportDiscardAll());
      assertEquals(0L, hi.himportDiscardAll());
    }
  }

  @Test
  public void noSuchFieldsetIsCommandError() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      JedisDataException e = assertThrows(JedisDataException.class,
        () -> hi.himportSet("k", "missing", "a", "b"));
      assertThat(e.getMessage(), equalTo("ERR no such fieldset"));
    }
  }

  @Test
  public void duplicateFieldIsCommandError() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      JedisDataException e = assertThrows(JedisDataException.class,
        () -> hi.himportPrepare("dup", "f1", "f1"));
      assertThat(e.getMessage(), equalTo("ERR duplicate field name in fieldset"));
    }
  }

  @Test
  public void valueCountMismatchIsCommandError() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      hi.himportPrepare("u", "name", "age");
      assertThrows(JedisDataException.class, () -> hi.himportSet("k", "u", "onlyone"));
    }
  }

  @Test
  public void wrongTypeIsCommandError() {
    jedis.set("plain", "hello");
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      hi.himportPrepare("u", "name", "age");
      JedisDataException e = assertThrows(JedisDataException.class,
        () -> hi.himportSet("plain", "u", "a", "b"));
      assertThat(e.getMessage(),
        equalTo("WRONGTYPE Operation against a key holding the wrong kind of value"));
    }
  }

  @Test
  public void sessionStaysActiveAfterCommandError() {
    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      // A command-level error does not break the session.
      assertThrows(JedisDataException.class, () -> hi.himportSet("k", "missing", "a"));
      // Subsequent commands on the same handle still work.
      assertEquals("OK", hi.himportPrepare("u", "name"));
      assertEquals("OK", hi.himportSet("user:1", "u", "alice"));
    }
    assertEquals("alice", jedis.hget("user:1", "name"));
  }

  @Test
  public void usageAfterCloseThrows() {
    AbstractHashImportHandler hi = jedis.hashImport();
    hi.himportPrepare("u", "f1");
    hi.close();
    JedisException e = assertThrows(JedisException.class, () -> hi.himportPrepare("u", "f1"));
    assertThat(e.getMessage(), equalTo("HashImport session is CLOSED"));
  }

  @Test
  public void binaryPrepareSetAndReadBack() {
    byte[] key = SafeEncoder.encode("buser:1");
    byte[] fs = SafeEncoder.encode("bu");
    byte[] fName = SafeEncoder.encode("name");
    byte[] fAge = SafeEncoder.encode("age");
    byte[] vName = SafeEncoder.encode("carol");
    byte[] vAge = SafeEncoder.encode("41");

    try (AbstractHashImportHandler hi = jedis.hashImport()) {
      assertEquals("OK", hi.himportPrepare(fs, fName, fAge));
      assertEquals("OK", hi.himportSet(key, fs, vName, vAge));
      assertEquals(1L, hi.himportDiscard(fs));
    }

    assertArrayEquals(vName, jedis.hget(key, fName));
  }
}
