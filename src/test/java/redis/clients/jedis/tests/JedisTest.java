package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.jedis.util.SafeEncoder;

public class JedisTest extends JedisCommandTestBase {

  @Test
  public void useWithoutConnecting() {
    try (Jedis j = new Jedis()) {
      j.auth("foobared");
      j.dbSize();
    }
  }

  @Test
  public void checkBinaryData() {
    byte[] bigdata = new byte[1777];
    for (int b = 0; b < bigdata.length; b++) {
      bigdata[b] = (byte) ((byte) b % 255);
    }
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("data", SafeEncoder.encode(bigdata));

    String status = jedis.hmset("foo", hash);
    assertEquals("OK", status);
    assertEquals(hash, jedis.hgetAll("foo"));
  }

  @Test
  public void connectWithShardInfo() {
    JedisShardInfo shardInfo = new JedisShardInfo("localhost", Protocol.DEFAULT_PORT);
    shardInfo.setPassword("foobared");
    Jedis jedis = new Jedis(shardInfo);
    jedis.get("foo");
    jedis.close();
  }

  @Test
  public void timeoutConnection() throws Exception {
    Jedis jedis = new Jedis("localhost", 6379, 15000);
    jedis.auth("foobared");
    String timeout = jedis.configGet("timeout").get(1);
    jedis.configSet("timeout", "1");
    Thread.sleep(2000);
    try {
      jedis.hmget("foobar", "foo");
      fail("Operation should throw JedisConnectionException");
    } catch(JedisConnectionException jce) {
      // expected
    }
    jedis.close();

    // reset config
    jedis = new Jedis("localhost", 6379);
    jedis.auth("foobared");
    jedis.configSet("timeout", timeout);
    jedis.close();
  }

  @Test
  public void timeoutConnectionWithURI() throws Exception {
    Jedis jedis = new Jedis(new URI("redis://:foobared@localhost:6380/2"), 15000);
    String timeout = jedis.configGet("timeout").get(1);
    jedis.configSet("timeout", "1");
    Thread.sleep(2000);
    try {
      jedis.hmget("foobar", "foo");
      fail("Operation should throw JedisConnectionException");
    } catch(JedisConnectionException jce) {
      // expected
    }
    jedis.close();

    // reset config
    jedis = new Jedis(new URI("redis://:foobared@localhost:6380/2"));
    jedis.configSet("timeout", timeout);
    jedis.close();
  }

  @Test
  public void infiniteTimeout() throws Exception {
    Jedis jedis = new Jedis("localhost", 6379, 350, 350, 350);
    jedis.auth("foobared");
    try {
      jedis.blpop(0, "foo");
      fail("SocketTimeoutException should occur");
    } catch(JedisConnectionException jce) {
      assertEquals(java.net.SocketTimeoutException.class, jce.getCause().getClass());
      assertEquals("Read timed out", jce.getCause().getMessage());
      assertTrue(jedis.getClient().isBroken());
    }
    jedis.close();
  }

  @Test(expected = JedisDataException.class)
  public void failWhenSendingNullValues() {
    jedis.set("foo", null);
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    Jedis j = new Jedis(new URI("localhost:6380"));
  }

  @Test
  public void shouldReconnectToSameDB() throws IOException {
    jedis.select(1);
    jedis.set("foo", "bar");
    jedis.getClient().getSocket().shutdownInput();
    jedis.getClient().getSocket().shutdownOutput();
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void startWithUrlString() {
    try (Jedis j = new Jedis("localhost", 6380)) {
      j.auth("foobared");
      j.select(2);
      j.set("foo", "bar");
    }

    try (Jedis j2 = new Jedis("redis://:foobared@localhost:6380/2")) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("foobared");
    j.select(2);
    j.set("foo", "bar");

    Jedis jedis = new Jedis(new URI("redis://:foobared@localhost:6380/2"));
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
  }

  @Test
  public void shouldNotUpdateDbIndexIfSelectFails() {
    int currentDb = jedis.getDB();
    try {
      int invalidDb = -1;
      jedis.select(invalidDb);

      fail("Should throw an exception if tried to select invalid db");
    } catch (JedisException e) {
      assertEquals(currentDb, jedis.getDB());
    }
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() {
    try (Jedis j1 = new Jedis("redis://localhost:6380")) {
      j1.auth("foobared");
      assertEquals("localhost", j1.getClient().getHost());
      assertEquals(6380, j1.getClient().getPort());
      assertEquals(0, j1.getDB());
    }

    try (Jedis j2 = new Jedis("redis://localhost:6380/")) {
      j2.auth("foobared");
      assertEquals("localhost", j2.getClient().getHost());
      assertEquals(6380, j2.getClient().getPort());
      assertEquals(0, j2.getDB());
    }
  }

  @Test
  public void checkCloseable() {
    jedis.close();
    BinaryJedis bj = new BinaryJedis("localhost");
    bj.connect();
    bj.close();
  }

  @Test
  public void checkDisconnectOnQuit() {
    jedis.quit();
    assertFalse(jedis.getClient().isConnected());
  }

}