package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.Protocol.Command.BLPOP;
import static redis.clients.jedis.Protocol.Command.GET;
import static redis.clients.jedis.Protocol.Command.LRANGE;
import static redis.clients.jedis.Protocol.Command.RPUSH;
import static redis.clients.jedis.Protocol.Command.SET;
import static redis.clients.jedis.params.SetParams.setParams;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.MSetExParams;

import redis.clients.jedis.util.SafeEncoder;

public abstract class BinaryValuesCommandsTestBase extends UnifiedJedisCommandsTestBase {
  protected byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  protected byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  protected byte[] bxx = { 0x78, 0x78 };
  protected byte[] bnx = { 0x6E, 0x78 };
  protected byte[] bex = { 0x65, 0x78 };
  protected byte[] bpx = { 0x70, 0x78 };
  protected int expireSeconds = 2;
  protected long expireMillis = expireSeconds * 1000;
  protected byte[] binaryValue;

  public BinaryValuesCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void startUp() {
    StringBuilder sb = new StringBuilder();

    for (int n = 0; n < 1000; n++) {
      sb.append("A");
    }

    binaryValue = sb.toString().getBytes();
  }

  @Test
  public void setAndGet() {
    assertEquals("OK", jedis.set(bfoo, binaryValue));

    assertArrayEquals(binaryValue, jedis.get(bfoo));

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setNxExAndGet() {
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().nx().ex(expireSeconds)));

    assertArrayEquals(binaryValue, jedis.get(bfoo));

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setIfNotExistAndGet() {
    assertEquals("OK", jedis.set(bfoo, binaryValue));
    // nx should fail if value exists
    assertNull(jedis.set(bfoo, binaryValue, setParams().nx().ex(expireSeconds)));

    assertArrayEquals(binaryValue, jedis.get(bfoo));

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setIfExistAndGet() {
    assertEquals("OK", jedis.set(bfoo, binaryValue));
    // nx should fail if value exists
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().xx().ex(expireSeconds)));

    byte[] value = jedis.get(bfoo);
    assertArrayEquals(binaryValue, value);

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setFailIfNotExistAndGet() {
    // xx should fail if value does NOT exists
    assertNull(jedis.set(bfoo, binaryValue, setParams().xx().ex(expireSeconds)));
  }

  @Test
  public void setAndExpireMillis() {
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().nx().px(expireMillis)));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void setAndExpire() {
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().nx().ex(expireSeconds)));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void setAndKeepttl() {
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().nx().ex(expireSeconds)));
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().keepttl()));
    assertEquals("OK", jedis.set(bfoo, binaryValue, setParams().keepTtl()));
    long ttl = jedis.ttl(bfoo);
    assertTrue(0 < ttl && ttl <= expireSeconds);
    jedis.set(bfoo, binaryValue);
    ttl = jedis.ttl(bfoo);
    assertTrue(ttl < 0);
  }

  @Test
  public void setAndPxat() {
    assertEquals("OK", jedis.set(bfoo, binaryValue,
      setParams().nx().pxAt(System.currentTimeMillis() + expireMillis)));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void setAndExat() {
    assertEquals("OK", jedis.set(bfoo, binaryValue,
      setParams().nx().exAt(System.currentTimeMillis() / 1000 + expireSeconds)));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void getSet() {
    assertNull(jedis.getSet(bfoo, binaryValue));
    assertArrayEquals(binaryValue, jedis.get(bfoo));
  }

  @Test
  public void getDel() {
    assertEquals("OK", jedis.set(bfoo, bbar));

    assertArrayEquals(bbar, jedis.getDel(bfoo));

    assertNull(jedis.get(bfoo));
  }

  @Test
  public void getEx() {
    assertNull(jedis.getEx(bfoo, GetExParams.getExParams().ex(1)));
    jedis.set(bfoo, bbar);

    assertArrayEquals(bbar, jedis.getEx(bfoo, GetExParams.getExParams().ex(10)));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= 10);

    assertArrayEquals(bbar, jedis.getEx(bfoo, GetExParams.getExParams().px(20000l)));
    ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 10 && ttl <= 20);

    assertArrayEquals(bbar,
      jedis.getEx(bfoo, GetExParams.getExParams().exAt(System.currentTimeMillis() / 1000 + 30)));
    ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 20 && ttl <= 30);

    assertArrayEquals(bbar,
      jedis.getEx(bfoo, GetExParams.getExParams().pxAt(System.currentTimeMillis() + 40000l)));
    ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 30 && ttl <= 40);

    assertArrayEquals(bbar, jedis.getEx(bfoo, GetExParams.getExParams().persist()));
    assertEquals(-1L, jedis.ttl(bfoo));
  }

  @Test
  public void mget() {
    List<byte[]> values = jedis.mget(bfoo, bbar);
    List<byte[]> expected = new ArrayList<>();
    expected.add(null);
    expected.add(null);

    assertByteArrayListEquals(expected, values);

    jedis.set(bfoo, binaryValue);

    expected = new ArrayList<>();
    expected.add(binaryValue);
    expected.add(null);
    assertByteArrayListEquals(expected, jedis.mget(bfoo, bbar));

    jedis.set(bbar, bfoo);

    expected = new ArrayList<>();
    expected.add(binaryValue);
    expected.add(bfoo);
    assertByteArrayListEquals(expected, jedis.mget(bfoo, bbar));
  }

  @Test
  public void setnx() {
    assertEquals(1, jedis.setnx(bfoo, binaryValue));
    assertArrayEquals(binaryValue, jedis.get(bfoo));

    assertEquals(0, jedis.setnx(bfoo, bbar));
    assertArrayEquals(binaryValue, jedis.get(bfoo));
  }

  @Test
  public void setex() {
    assertEquals("OK", jedis.setex(bfoo, 20, binaryValue));
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= 20);
  }

  @Test
  public void mset() {
    assertEquals("OK", jedis.mset(bfoo, binaryValue, bbar, bfoo));
    assertArrayEquals(binaryValue, jedis.get(bfoo));
    assertArrayEquals(bfoo, jedis.get(bbar));
  }

  @Test
  public void msetnx() {
    assertEquals(1, jedis.msetnx(bfoo, binaryValue, bbar, bfoo));
    assertArrayEquals(binaryValue, jedis.get(bfoo));
    assertArrayEquals(bfoo, jedis.get(bbar));

    assertEquals(0, jedis.msetnx(bfoo, bbar, "bar2".getBytes(), "foo2".getBytes()));
    assertArrayEquals(binaryValue, jedis.get(bfoo));
    assertArrayEquals(bfoo, jedis.get(bbar));
  }

  @Test
  public void incr() {
    assertEquals(1, jedis.incr(bfoo));
    assertEquals(2, jedis.incr(bfoo));
  }

  @Test
  public void incrWrongValue() {
    jedis.set(bfoo, binaryValue);
    assertThrows(JedisDataException.class, () -> jedis.incr(bfoo));
  }

  @Test
  public void incrBy() {
    assertEquals(2, jedis.incrBy(bfoo, 2));
    assertEquals(4, jedis.incrBy(bfoo, 2));
  }

  @Test
  public void incrByWrongValue() {
    jedis.set(bfoo, binaryValue);
    assertThrows(JedisDataException.class, () -> jedis.incrBy(bfoo, 2));
  }

  @Test
  public void incrByFloat() {
    assertEquals(10.5, jedis.incrByFloat(bfoo, 10.5), 0.0);
    assertEquals(10.6, jedis.incrByFloat(bfoo, 0.1), 0.0);
  }

  @Test
  public void decr() {
    assertEquals(-1, jedis.decr(bfoo));
    assertEquals(-2, jedis.decr(bfoo));
  }

  @Test
  public void decrWrongValue() {
    jedis.set(bfoo, binaryValue);
    assertThrows(JedisDataException.class, () -> jedis.decr(bfoo));
  }

  @Test
  public void decrBy() {
    assertEquals(-2, jedis.decrBy(bfoo, 2));
    assertEquals(-4, jedis.decrBy(bfoo, 2));
  }

  @Test
  public void decrByWrongValue() {
    jedis.set(bfoo, binaryValue);
    assertThrows(JedisDataException.class, () -> jedis.decrBy(bfoo, 2));
  }

  @Test
  public void append() {
    byte[] first512 = new byte[512];
    System.arraycopy(binaryValue, 0, first512, 0, 512);
    assertEquals(512, jedis.append(bfoo, first512));
    assertArrayEquals(first512, jedis.get(bfoo));

    byte[] rest = new byte[binaryValue.length - 512];
    System.arraycopy(binaryValue, 512, rest, 0, binaryValue.length - 512);
    assertEquals(binaryValue.length, jedis.append(bfoo, rest));

    assertArrayEquals(binaryValue, jedis.get(bfoo));
  }

  @Test
  public void substr() {
    jedis.set(bfoo, binaryValue);

    byte[] first512 = new byte[512];
    System.arraycopy(binaryValue, 0, first512, 0, 512);
    byte[] rfirst512 = jedis.substr(bfoo, 0, 511);
    assertArrayEquals(first512, rfirst512);

    byte[] last512 = new byte[512];
    System.arraycopy(binaryValue, binaryValue.length - 512, last512, 0, 512);
    assertArrayEquals(last512, jedis.substr(bfoo, -512, -1));

    assertArrayEquals(binaryValue, jedis.substr(bfoo, 0, -1));

    assertArrayEquals(last512, jedis.substr(bfoo, binaryValue.length - 512, 100000));
  }

  @Test
  public void strlen() {
    jedis.set(bfoo, binaryValue);
    assertEquals(binaryValue.length, jedis.strlen(bfoo));
  }

  @Test
  public void setGet() {
    assertEquals("OK", jedis.set(bfoo, bbar));

    // GET old value
    assertArrayEquals(bbar, jedis.setGet(bfoo, binaryValue));

    assertArrayEquals(binaryValue, jedis.get(bfoo));

    // GET null value
    assertNull(jedis.setGet(bbar, bfoo));
  }

  @Test
  public void setGetWithParams() {
    jedis.del(bfoo);

    // no previous, return null
    assertNull(jedis.setGet(bfoo, bbar, setParams().nx()));

    // key already exists, new value should not be set, previous value should be bbar
    assertArrayEquals(bbar, jedis.setGet(bfoo, binaryValue, setParams().nx()));

    assertArrayEquals(bbar, jedis.setGet(bfoo, binaryValue, setParams().xx()));
  }

  /**
   * Tests the executeCommand method with CommandArguments for proper cluster routing. This test
   * uses explicit key marking through CommandArguments.key() for cluster compatibility.
   */
  @Test
  public void executeCommandTest() {
    // Test SET command with proper key marking
    Object obj = jedis
        .executeCommand(new CommandArguments(SET).key("x".getBytes()).add("1".getBytes()));
    String returnValue = SafeEncoder.encode((byte[]) obj);
    assertEquals("OK", returnValue);

    // Test GET command with proper key marking
    obj = jedis.executeCommand(new CommandArguments(GET).key("x".getBytes()));
    returnValue = SafeEncoder.encode((byte[]) obj);
    assertEquals("1", returnValue);

    // Test RPUSH commands with proper key marking
    jedis.executeCommand(new CommandArguments(RPUSH).key("foo".getBytes()).add("a".getBytes()));
    jedis.executeCommand(new CommandArguments(RPUSH).key("foo".getBytes()).add("b".getBytes()));
    jedis.executeCommand(new CommandArguments(RPUSH).key("foo".getBytes()).add("c".getBytes()));

    // Test LRANGE command with proper key marking
    obj = jedis.executeCommand(
      new CommandArguments(LRANGE).key("foo".getBytes()).add("0".getBytes()).add("2".getBytes()));
    List<byte[]> list = (List<byte[]>) obj;
    List<byte[]> expected = new ArrayList<>(3);
    expected.add("a".getBytes());
    expected.add("b".getBytes());
    expected.add("c".getBytes());
    for (int i = 0; i < 3; i++)
      assertArrayEquals(expected.get(i), list.get(i));
  }

  /**
   * Tests the executeCommand method with blocking CommandArguments for proper cluster routing. This
   * test uses explicit key marking through CommandArguments.key() and .blocking() for cluster
   * compatibility with blocking operations.
   */
  @Test
  public void executeBlockingCommandTest() {
    // Test BLPOP on empty list - should return null after timeout
    assertNull(jedis.executeCommand(
      new CommandArguments(BLPOP).key(bfoo).add(Protocol.toByteArray(1L)).blocking()));

    // Setup: push an element to the list using executeCommand with proper key marking
    jedis.executeCommand(new CommandArguments(RPUSH).key(bfoo).add(bbar));

    // Test BLPOP with data - should return the key and value
    List<byte[]> blpop = (List<byte[]>) jedis.executeCommand(
      new CommandArguments(BLPOP).key(bfoo).add(Protocol.toByteArray(1L)).blocking());
    assertEquals(2, blpop.size());
    assertArrayEquals(bfoo, blpop.get(0));
    assertArrayEquals(bbar, blpop.get(1));

    // Test BLPOP on now-empty list - should return null after timeout
    assertNull(jedis.executeCommand(
      new CommandArguments(BLPOP).key(bfoo).add(Protocol.toByteArray(1L)).blocking()));
  }

  // MSETEX NX + expiration matrix (binary)
  static Stream<Arguments> msetexNxArgsProvider() {
    return Stream.of(Arguments.of("EX", new MSetExParams().nx().ex(5)),
      Arguments.of("PX", new MSetExParams().nx().px(5000)),
      Arguments.of("EXAT", new MSetExParams().nx().exAt(System.currentTimeMillis() / 1000 + 5)),
      Arguments.of("PXAT", new MSetExParams().nx().pxAt(System.currentTimeMillis() + 5000)),
      Arguments.of("KEEPTTL", new MSetExParams().nx().keepTtl()));
  }

  @ParameterizedTest(name = "MSETEX NX + {0} (binary)")
  @MethodSource("msetexNxArgsProvider")
  @EnabledOnCommand("MSETEX")
  public void msetexNx_binary_parametrized(String optionLabel, MSetExParams params) {
    byte[] k1 = "{t}msetex:unifiedb:k1".getBytes();
    byte[] k2 = "{t}msetex:unifiedb:k2".getBytes();

    boolean result = jedis.msetex(params, k1, "v1".getBytes(), k2, "v2".getBytes());
    assertTrue(result);

    long ttl = jedis.ttl(k1);
    if ("KEEPTTL".equals(optionLabel)) {
      assertEquals(-1L, ttl);
    } else {
      assertTrue(ttl > 0L);
    }
  }
}
