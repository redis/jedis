package redis.clients.jedis.tests;

import org.junit.Before;
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
import redis.clients.jedis.tests.utils.RedisVersionUtil;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * This test class is a copy of @JedisTest where all authentications are made with
 * default:foobared credentialsinformation
 *
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class JedisWithCompleteCredentialsTest extends JedisCommandTestBase {

  /**
   * Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    boolean shouldNotRun = ((new RedisVersionUtil(jedis)).getRedisMajorVersionNumber() < 6);
    if ( shouldNotRun ) {
      org.junit.Assume.assumeFalse("Not running ACL tests on this version of Redis", shouldNotRun);
    }
  }

  @Test
  public void useWithoutConnecting() {
    Jedis jedis = new Jedis("localhost");
    jedis.auth("default", "foobared");
    jedis.dbSize();
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
    shardInfo.setUser("default");
    shardInfo.setPassword("foobared");
    Jedis jedis = new Jedis(shardInfo);
    jedis.get("foo");
  }

  @Test
  public void timeoutConnection() throws Exception {
    Jedis jedis = new Jedis("localhost", 6379, 15000);
    jedis.auth("default", "foobared");
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
    jedis.auth("default", "foobared");
    jedis.configSet("timeout", timeout);
    jedis.close();
  }

  @Test
  public void timeoutConnectionWithURI() throws Exception {
    Jedis jedis = new Jedis(new URI("redis://default:foobared@localhost:6380/2"), 15000);
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
    jedis = new Jedis(new URI("redis://default:foobared@localhost:6380/2"));
    jedis.configSet("timeout", timeout);
    jedis.close();
  }

  @Test(expected = JedisDataException.class)
  public void failWhenSendingNullValues() {
    jedis.set("foo", null);
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    Jedis j = new Jedis(new URI("localhost:6380"));
    j.ping();
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
    Jedis j = new Jedis("localhost", 6380);
    j.auth("default", "foobared");
    j.select(2);
    j.set("foo", "bar");
    Jedis jedis = new Jedis("redis://default:foobared@localhost:6380/2");
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("default","foobared");
    j.select(2);
    j.set("foo", "bar");
    Jedis jedis = new Jedis(new URI("redis://default:foobared@localhost:6380/2"));
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void shouldNotUpdateDbIndexIfSelectFails() throws URISyntaxException {
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
  public void connectWithURICredentials() throws URISyntaxException {
    Jedis j = new Jedis("localhost");
    j.auth("default", "foobared");
    j.set("foo", "bar");

    // create new user
    j.aclSetUser("alice", "on", ">alicePassword", "~*", "+@all");

    Jedis jedis = new Jedis(new URI("redis://default:foobared@localhost:6379"));
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));

    Jedis jedis2 = new Jedis(new URI("redis://alice:alicePassword@localhost:6379"));
    assertEquals("PONG", jedis2.ping());
    assertEquals("bar", jedis2.get("foo"));

    // delete user
    j.aclDelUser("alice");
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() {
    Jedis jedis = new Jedis("redis://localhost:6380");
    jedis.auth("default", "foobared");
    assertEquals("localhost", jedis.getClient().getHost());
    assertEquals(6380, jedis.getClient().getPort());
    assertEquals(0, jedis.getDB());

    jedis = new Jedis("redis://localhost:6380/");
    jedis.auth("default", "foobared");
    assertEquals("localhost", jedis.getClient().getHost());
    assertEquals(6380, jedis.getClient().getPort());
    assertEquals(0, jedis.getDB());
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