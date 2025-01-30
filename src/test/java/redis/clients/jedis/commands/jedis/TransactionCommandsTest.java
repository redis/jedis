package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import static redis.clients.jedis.Protocol.Command.INCR;
import static redis.clients.jedis.Protocol.Command.GET;
import static redis.clients.jedis.Protocol.Command.SET;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class TransactionCommandsTest extends JedisCommandsTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };

  final byte[] bmykey = { 0x42, 0x02, 0x03, 0x04 };

  Jedis nj;

  public TransactionCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    nj = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().timeoutMillis(500).build());
  }

  @After
  @Override
  public void tearDown() throws Exception {
    nj.close();
    super.tearDown();
  }

  @Test
  public void multi() {
    Transaction trans = jedis.multi();

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
    trans = jedis.multi();

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
  public void watch() throws UnknownHostException, IOException {
    jedis.watch("mykey", "somekey");
    Transaction t = jedis.multi();

    nj.set("mykey", "bar");

    t.set("mykey", "foo");
    List<Object> resp = t.exec();
    assertNull(resp);
    assertEquals("bar", jedis.get("mykey"));

    // Binary
    jedis.watch(bmykey, "foobar".getBytes());
    t = jedis.multi();

    nj.set(bmykey, bbar);

    t.set(bmykey, bfoo);
    resp = t.exec();
    assertNull(resp);
    assertArrayEquals(bbar, jedis.get(bmykey));
  }

  @Test
  public void unwatch() {
    jedis.watch("mykey");
    jedis.get("mykey");
    String val = "foo";
    assertEquals("OK", jedis.unwatch());
    Transaction t = jedis.multi();

    nj.set("mykey", "bar");

    t.set("mykey", val);
    List<Object> resp = t.exec();
    assertEquals(1, resp.size());
    assertEquals("OK", resp.get(0));

    // Binary
    jedis.watch(bmykey);
    jedis.get(bmykey);
    byte[] bval = bfoo;
    assertEquals("OK", jedis.unwatch());
    t = jedis.multi();

    nj.set(bmykey, bbar);

    t.set(bmykey, bval);
    resp = t.exec();
    assertEquals(1, resp.size());
    assertEquals("OK", resp.get(0));
  }

  @Test(expected = IllegalStateException.class)
  public void validateWhenInMulti() {
    jedis.multi();
    jedis.ping();
  }

  @Test
  public void discard() {
    Transaction t = jedis.multi();
    String status = t.discard();
    assertEquals("OK", status);
  }

  @Test
  public void discardFail() {
    Transaction trans = jedis.multi();
    trans.set("a", "a");
    trans.set("b", "b");

    try (MockedStatic<Protocol> protocol = Mockito.mockStatic(Protocol.class)) {
      protocol.when(() -> Protocol.read(any())).thenThrow(JedisConnectionException.class);

      trans.discard();
      fail("Should get mocked JedisConnectionException.");
    } catch (JedisConnectionException jce) {
      // should be here
    } finally {
      // close() should pass
      trans.close();
    }
    assertTrue(jedis.isBroken());
  }

  @Test
  public void execFail() {
    Transaction trans = jedis.multi();
    trans.set("a", "a");
    trans.set("b", "b");

    try (MockedStatic<Protocol> protocol = Mockito.mockStatic(Protocol.class)) {
      protocol.when(() -> Protocol.read(any())).thenThrow(JedisConnectionException.class);

      trans.exec();
      fail("Should get mocked JedisConnectionException.");
    } catch (JedisConnectionException jce) {
      // should be here
    } finally {
      // close() should pass
      trans.close();
    }
    assertTrue(jedis.isBroken());
  }

  @Test
  public void transactionResponse() {
    jedis.set("string", "foo");
    jedis.lpush("list", "foo");
    jedis.hset("hash", "foo", "bar");
    jedis.zadd("zset", 1, "foo");
    jedis.sadd("set", "foo");

    Transaction t = jedis.multi();
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
    jedis.set("string", "foo");
    jedis.lpush("list", "foo");
    jedis.hset("hash", "foo", "bar");
    jedis.zadd("zset", 1, "foo");
    jedis.sadd("set", "foo");

    Transaction t = jedis.multi();
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
    jedis.set("string", "foo");

    Transaction t = jedis.multi();
    Response<String> string = t.get("string");
    string.get();
    t.exec();
  }

  @Test
  public void transactionResponseWithError() {
    Transaction t = jedis.multi();
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
//
//  @Test
//  public void execGetResponse() {
//    Transaction t = jedis.multi();
//
//    t.set("foo", "bar");
//    t.smembers("foo");
//    t.get("foo");
//
//    List<Response<?>> lr = t.execGetResponse();
//    try {
//      lr.get(1).get();
//      fail("We expect exception here!");
//    } catch (JedisDataException e) {
//      // that is fine we should be here
//    }
//    assertEquals("bar", lr.get(2).get());
//  }
//
//  @Test
//  public void select() {
//    jedis.select(1);
//    jedis.set("foo", "bar");
//    jedis.watch("foo");
//    Transaction t = jedis.multi();
//    t.select(0);
//    t.set("bar", "foo");
//
//    Jedis jedis2 = createJedis();
//    jedis2.select(1);
//    jedis2.set("foo", "bar2");
//
//    List<Object> results = t.exec();
//    assertNull(results);
//  }

  @Test
  public void testResetStateWhenInMulti() {
    Transaction t = jedis.multi();
    t.set("foooo", "barrr");

    jedis.resetState();
    assertNull(jedis.get("foooo"));
  }
//
//  @Test
//  public void testResetStateWhenInMultiWithinPipeline() {
//    Pipeline p = jedis.pipelined();
//    p.multi();
//    p.set("foooo", "barrr");
//
//    jedis.resetState();
//    assertNull(jedis.get("foooo"));
//  }

  @Test
  public void testResetStateWhenInWatch() {
    jedis.watch("mykey", "somekey");

    // state reset : unwatch
    jedis.resetState();

    Transaction t = jedis.multi();

    nj.set("mykey", "bar");

    t.set("mykey", "foo");
    List<Object> resp = t.exec();
    assertNotNull(resp);
    assertEquals(1, resp.size());
    assertEquals("foo", jedis.get("mykey"));
  }

  @Test
  public void testResetStateWithFullyExecutedTransaction() {
    Jedis jedis2 = createJedis();

    Transaction t = jedis2.multi();
    t.set("mykey", "foo");
    t.get("mykey");

    List<Object> resp = t.exec();
    assertNotNull(resp);
    assertEquals(2, resp.size());

    jedis2.resetState();
    jedis2.close();
  }

  @Test
  public void testCloseable() {
    // we need to test with fresh instance of Jedis
    Jedis jedis2 = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().timeoutMillis(500).build());;

    Transaction transaction = jedis2.multi();
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
    Transaction t = jedis.multi();
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
    Transaction t = jedis.multi();
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
}