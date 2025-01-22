package redis.clients.jedis.commands.unified.cluster;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.commands.unified.ListCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisVersionRule;

@RunWith(Parameterized.class)
public class ClusterListCommandsTest extends ListCommandsTestBase {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

  public ClusterListCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @After
  public void tearDown() {
    jedis.close();
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @Override
  public void rpoplpush() {
    jedis.rpush("foo{|}", "a");
    jedis.rpush("foo{|}", "b");
    jedis.rpush("foo{|}", "c");

    jedis.rpush("dst{|}", "foo");
    jedis.rpush("dst{|}", "bar");

    String element = jedis.rpoplpush("foo{|}", "dst{|}");

    assertEquals("c", element);

    List<String> srcExpected = new ArrayList<>();
    srcExpected.add("a");
    srcExpected.add("b");

    List<String> dstExpected = new ArrayList<>();
    dstExpected.add("c");
    dstExpected.add("foo");
    dstExpected.add("bar");

    assertEquals(srcExpected, jedis.lrange("foo{|}", 0, 1000));
    assertEquals(dstExpected, jedis.lrange("dst{|}", 0, 1000));
  }

  @Test
  @Override
  public void blpop() throws InterruptedException {
    List<String> result = jedis.blpop(1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.blpop(1, "foo");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo", result.get(0));
    assertEquals("bar", result.get(1));

    // Multi keys
    result = jedis.blpop(1, "{foo}", "{foo}1");
    assertNull(result);

    jedis.lpush("{foo}", "bar");
    jedis.lpush("{foo}1", "bar1");
    result = jedis.blpop(1, "{foo}1", "{foo}");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("{foo}1", result.get(0));
    assertEquals("bar1", result.get(1));

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.blpop(1, bfoo);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  @Override
  public void blpopDouble() throws InterruptedException {
    KeyValue<String, String> result = jedis.blpop(0.1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.blpop(3.2, "foo");

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getValue());

    // Multi keys
    result = jedis.blpop(0.18, "{foo}", "{foo}1");
    assertNull(result);

    jedis.lpush("{foo}", "bar");
    jedis.lpush("{foo}1", "bar1");
    result = jedis.blpop(1d, "{foo}1", "{foo}");

    assertNotNull(result);
    assertEquals("{foo}1", result.getKey());
    assertEquals("bar1", result.getValue());

    // Binary
    jedis.lpush(bfoo, bbar);
    KeyValue<byte[], byte[]> bresult = jedis.blpop(3.12, bfoo);

    assertNotNull(bresult);
    assertArrayEquals(bfoo, bresult.getKey());
    assertArrayEquals(bbar, bresult.getValue());
  }

  @Test
  @Override
  public void brpop() throws InterruptedException {
    List<String> result = jedis.brpop(1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.brpop(1, "foo");
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo", result.get(0));
    assertEquals("bar", result.get(1));

    // Multi keys
    result = jedis.brpop(1, "{foo}", "{foo}1");
    assertNull(result);

    jedis.lpush("{foo}", "bar");
    jedis.lpush("{foo}1", "bar1");
    result = jedis.brpop(1, "{foo}1", "{foo}");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("{foo}1", result.get(0));
    assertEquals("bar1", result.get(1));

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.brpop(1, bfoo);
    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  @Override
  public void brpopDouble() throws InterruptedException {
    KeyValue<String, String> result = jedis.brpop(0.1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.brpop(3.2, "foo");

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getValue());

    // Multi keys
    result = jedis.brpop(0.18, "{foo}", "{foo}1");
    assertNull(result);

    jedis.lpush("{foo}", "bar");
    jedis.lpush("{foo}1", "bar1");
    result = jedis.brpop(1d, "{foo}1", "{foo}");

    assertNotNull(result);
    assertEquals("{foo}1", result.getKey());
    assertEquals("bar1", result.getValue());

    // Binary
    jedis.lpush(bfoo, bbar);
    KeyValue<byte[], byte[]> bresult = jedis.brpop(3.12, bfoo);

    assertNotNull(bresult);
    assertArrayEquals(bfoo, bresult.getKey());
    assertArrayEquals(bbar, bresult.getValue());
  }

  @Test
  @Override
  public void brpoplpush() {

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
        jedis.lpush("foo{|}", "a");
      }
    }).start();

    String element = jedis.brpoplpush("foo{|}", "bar{|}", 0);

    assertEquals("a", element);
    assertEquals(1, jedis.llen("bar{|}"));
    assertEquals("a", jedis.lrange("bar{|}", 0, -1).get(0));
  }

  @Test
  @Override
  public void lmove() {
    jedis.rpush("{|}foo", "bar1", "bar2", "bar3");
    assertEquals("bar3", jedis.lmove("{|}foo", "{|}bar", ListDirection.RIGHT, ListDirection.LEFT));
    assertEquals(Collections.singletonList("bar3"), jedis.lrange("{|}bar", 0, -1));
    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("{|}foo", 0, -1));
  }

  @Test
  @Override
  public void blmove() {
    new Thread(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      jedis.rpush("{|}foo", "bar1", "bar2", "bar3");
    }).start();

    assertEquals("bar3", jedis.blmove("{|}foo", "{|}bar", ListDirection.RIGHT, ListDirection.LEFT, 0));
    assertEquals(Collections.singletonList("bar3"), jedis.lrange("{|}bar", 0, -1));
    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("{|}foo", 0, -1));
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void lmpop() {
    String mylist1 = "mylist1{.}";
    String mylist2 = "mylist2{.}";

    // add elements to list
    jedis.lpush(mylist1, "one", "two", "three", "four", "five");
    jedis.lpush(mylist2, "one", "two", "three", "four", "five");

    KeyValue<String, List<String>> elements = jedis.lmpop(ListDirection.LEFT, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(1, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.LEFT, 5, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(4, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.RIGHT, 100, mylist1, mylist2);
    assertEquals(mylist2, elements.getKey());
    assertEquals(5, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.RIGHT, mylist1, mylist2);
    assertNull(elements);
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void blmpopSimple() {
    String mylist1 = "mylist1{.}";
    String mylist2 = "mylist2{.}";

    // add elements to list
    jedis.lpush(mylist1, "one", "two", "three", "four", "five");
    jedis.lpush(mylist2, "one", "two", "three", "four", "five");

    KeyValue<String, List<String>> elements = jedis.blmpop(1L, ListDirection.LEFT, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(1, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.LEFT, 5, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(4, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.RIGHT, 100, mylist1, mylist2);
    assertEquals(mylist2, elements.getKey());
    assertEquals(5, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.RIGHT, mylist1, mylist2);
    assertNull(elements);
  }
}
