package redis.clients.jedis;

import static org.junit.Assert.*;
import static redis.clients.jedis.Protocol.Command.INCR;
import static redis.clients.jedis.Protocol.Command.GET;
import static redis.clients.jedis.Protocol.Command.SET;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

public class TransactionV2Test {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };

  final byte[] bmykey = { 0x42, 0x02, 0x03, 0x04 };

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  private Connection conn;
  private Jedis nj;

  @Before
  public void setUp() throws Exception {
    conn = new Connection(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().timeoutMillis(500).build());

    nj = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().timeoutMillis(500).build());

    nj.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    nj.close();
    conn.close();
  }

  @Test
  public void multi() {
    Transaction trans = new Transaction(conn);

    trans.sadd("foo", "a");
    trans.sadd("foo", "b");
    trans.scard("foo");

    List<Object> response = trans.exec();

    List<Object> expected = new ArrayList<Object>();
    expected.add(1L);
    expected.add(1L);
    expected.add(2L);
    assertEquals(expected, response);

    // Binary
    trans = new Transaction(conn);

    trans.sadd(bfoo, ba);
    trans.sadd(bfoo, bb);
    trans.scard(bfoo);

    response = trans.exec();

    expected = new ArrayList<Object>();
    expected.add(1L);
    expected.add(1L);
    expected.add(2L);
    assertEquals(expected, response);
  }

  @Test
  public void watch() {
    Transaction t = new Transaction(conn, false);
    assertEquals("OK", t.watch("mykey", "somekey"));
    t.multi();

    nj.set("mykey", "bar");

    t.set("mykey", "foo");
    List<Object> resp = t.exec();
    assertNull(resp);
    assertEquals("bar", nj.get("mykey"));

    // Binary
    assertEquals("OK", t.watch(bmykey, "foobar".getBytes()));
    t.multi();

    nj.set(bmykey, bbar);

    t.set(bmykey, bfoo);
    resp = t.exec();
    assertNull(resp);
    assertArrayEquals(bbar, nj.get(bmykey));
  }

  @Test
  public void unwatch() {
    Transaction t = new Transaction(conn, false);
    assertEquals("OK", t.watch("mykey"));
    String val = "foo";
    assertEquals("OK", t.unwatch());
    t.multi();

    nj.set("mykey", "bar");

    t.set("mykey", val);
    List<Object> resp = t.exec();
    assertEquals(1, resp.size());
    assertEquals("OK", resp.get(0));

    // Binary
    t.watch(bmykey);
    byte[] bval = bfoo;
    assertEquals("OK", t.unwatch());
    t.multi();

    nj.set(bmykey, bbar);

    t.set(bmykey, bval);
    resp = t.exec();
    assertEquals(1, resp.size());
    assertEquals("OK", resp.get(0));
  }

  @Test
  public void discard() {
    Transaction t = new Transaction(conn);
    String status = t.discard();
    assertEquals("OK", status);
  }

  @Test
  public void transactionResponse() {
    nj.set("string", "foo");
    nj.lpush("list", "foo");
    nj.hset("hash", "foo", "bar");
    nj.zadd("zset", 1, "foo");
    nj.sadd("set", "foo");

    Transaction t = new Transaction(conn);
    Response<String> string = t.get("string");
    Response<String> list = t.lpop("list");
    Response<String> hash = t.hget("hash", "foo");
    Response<List<String>> zset = t.zrange("zset", 0, -1);
    Response<String> set = t.spop("set");
    t.exec();

    assertEquals("foo", string.get());
    assertEquals("foo", list.get());
    assertEquals("bar", hash.get());
    assertEquals("foo", zset.get().iterator().next());
    assertEquals("foo", set.get());
  }

  @Test
  public void transactionResponseBinary() {
    nj.set("string", "foo");
    nj.lpush("list", "foo");
    nj.hset("hash", "foo", "bar");
    nj.zadd("zset", 1, "foo");
    nj.sadd("set", "foo");

    Transaction t = new Transaction(conn);
    Response<byte[]> string = t.get("string".getBytes());
    Response<byte[]> list = t.lpop("list".getBytes());
    Response<byte[]> hash = t.hget("hash".getBytes(), "foo".getBytes());
    Response<List<byte[]>> zset = t.zrange("zset".getBytes(), 0, -1);
    Response<byte[]> set = t.spop("set".getBytes());
    t.exec();

    assertArrayEquals("foo".getBytes(), string.get());
    assertArrayEquals("foo".getBytes(), list.get());
    assertArrayEquals("bar".getBytes(), hash.get());
    assertArrayEquals("foo".getBytes(), zset.get().iterator().next());
    assertArrayEquals("foo".getBytes(), set.get());
  }

  @Test(expected = IllegalStateException.class)
  public void transactionResponseWithinPipeline() {
    nj.set("string", "foo");

    Transaction t = new Transaction(conn);
    Response<String> string = t.get("string");
    string.get();
    t.exec();
  }

  @Test
  public void transactionResponseWithError() {
    Transaction t = new Transaction(conn);
    t.set("foo", "bar");
    Response<Set<String>> error = t.smembers("foo");
    Response<String> r = t.get("foo");
    List<Object> l = t.exec();
    assertSame(JedisDataException.class, l.get(1).getClass());
    try {
      error.get();
      fail("We expect exception here!");
    } catch (JedisDataException e) {
      // that is fine we should be here
    }
    assertEquals("bar", r.get());
  }

  @Test
  public void testCloseable() {
    Transaction transaction = new Transaction(conn);
    transaction.set("a", "1");
    transaction.set("b", "2");

    transaction.close();

    try {
      transaction.exec();
      fail("close should discard transaction");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("EXEC without MULTI"));
      // pass
    }
  }

  @Test
  public void testTransactionWithGeneralCommand() {
    Transaction t = new Transaction(conn);
    t.set("string", "foo");
    t.lpush("list", "foo");
    t.hset("hash", "foo", "bar");
    t.zadd("zset", 1, "foo");
    t.sendCommand(SET, "x", "1");
    t.sadd("set", "foo");
    t.sendCommand(INCR, "x");
    Response<String> string = t.get("string");
    Response<String> list = t.lpop("list");
    Response<String> hash = t.hget("hash", "foo");
    Response<List<String>> zset = t.zrange("zset", 0, -1);
    Response<String> set = t.spop("set");
    Response<Object> x = t.sendCommand(GET, "x");
    t.exec();

    assertEquals("foo", string.get());
    assertEquals("foo", list.get());
    assertEquals("bar", hash.get());
    assertEquals("foo", zset.get().iterator().next());
    assertEquals("foo", set.get());
    assertEquals("2", SafeEncoder.encode((byte[]) x.get()));
  }

  @Test
  public void transactionResponseWithErrorWithGeneralCommand() {
    Transaction t = new Transaction(conn);
    t.set("foo", "bar");
    t.sendCommand(SET, "x", "1");
    Response<Set<String>> error = t.smembers("foo");
    Response<String> r = t.get("foo");
    Response<Object> x = t.sendCommand(GET, "x");
    t.sendCommand(INCR, "x");
    List<Object> l = t.exec();
    assertSame(JedisDataException.class, l.get(2).getClass());
    try {
      error.get();
      fail("We expect exception here!");
    } catch (JedisDataException e) {
      // that is fine we should be here
    }
    assertEquals("bar", r.get());
    assertEquals("1", SafeEncoder.encode((byte[]) x.get()));
  }

  @Test
  public void zrevrangebyscore() {
    nj.zadd("foo", 1.0d, "a");
    nj.zadd("foo", 2.0d, "b");
    nj.zadd("foo", 3.0d, "c");
    nj.zadd("foo", 4.0d, "d");
    nj.zadd("foo", 5.0d, "e");

    Transaction t = new Transaction(conn);
    Response<List<String>> range = t.zrevrangeByScore("foo", 3d, Double.NEGATIVE_INFINITY, 0, 1);
    t.exec();
    List<String> expected = new ArrayList<String>();
    expected.add("c");

    assertEquals(expected, range.get());

    t = new Transaction(conn);
    range = t.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 0, 2);
    t.exec();
    expected = new ArrayList<String>();
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range.get());

    t = new Transaction(conn);
    range = t.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 1, 1);
    t.exec();
    expected = new ArrayList<String>();
    expected.add("b");

    assertEquals(expected, range.get());

    t = new Transaction(conn);
    range = t.zrevrangeByScore("foo", 4d, 2d);
    t.exec();
    expected = new ArrayList<String>();
    expected.add("d");
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range.get());

    t = new Transaction(conn);
    range = t.zrevrangeByScore("foo", "+inf", "(4");
    t.exec();
    expected = new ArrayList<String>();
    expected.add("e");

    assertEquals(expected, range.get());

    // Binary
    nj.zadd(bfoo, 1d, ba);
    nj.zadd(bfoo, 10d, bb);
    nj.zadd(bfoo, 0.1d, bc);
    nj.zadd(bfoo, 2d, ba);

    t = new Transaction(conn);
    Response<List<byte[]>> brange = t.zrevrangeByScore(bfoo, 2d, 0d);
    t.exec();

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, brange.get());

    t = new Transaction(conn);
    brange = t.zrevrangeByScore(bfoo, 2d, 0d, 0, 1);
    t.exec();

    bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);

    assertByteArrayListEquals(bexpected, brange.get());

    t = new Transaction(conn);
    Response<List<byte[]>> brange2 = t.zrevrangeByScore(bfoo, SafeEncoder.encode("+inf"),
        SafeEncoder.encode("(2"));
    t.exec();

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);

    assertByteArrayListEquals(bexpected, brange2.get());

    t = new Transaction(conn);
    brange = t.zrevrangeByScore(bfoo, 2d, 0d, 1, 1);
    t.exec();
    bexpected = new ArrayList<byte[]>();
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, brange.get());
  }

}