package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanIteration;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.resps.ScanResult;

public class ClusterValuesCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void nullKeys() {
    byte[] bfoo = new byte[]{0x0b, 0x0f, 0x00, 0x00};

    try {
      cluster.exists((byte[]) null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      cluster.exists(bfoo, null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      cluster.exists(null, bfoo);
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testHincrByFloat() {
    Double value = cluster.hincrByFloat("foo", "bar", 1.5d);
    assertEquals((Double) 1.5d, value);
    value = cluster.hincrByFloat("foo", "bar", -1.5d);
    assertEquals((Double) 0d, value);
    value = cluster.hincrByFloat("foo", "bar", -10.7d);
    assertEquals(Double.valueOf(-10.7d), value);
  }

  @Test
  public void georadiusStore() {
    // prepare datas
    Map<String, GeoCoordinate> coordinateMap = new HashMap<String, GeoCoordinate>();
    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    cluster.geoadd("{Sicily}", coordinateMap);

    long size = cluster.georadiusStore("{Sicily}", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("{Sicily}Store"));
    assertEquals(2, size);
    List<String> expected = new ArrayList<String>();
    expected.add("Palermo");
    expected.add("Catania");
    assertEquals(expected, cluster.zrange("{Sicily}Store", 0, -1));
  }

  private void publishOne(final String channel, final String message) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          cluster.publish(channel, message);
        } catch (Exception ex) {
        }
      }
    });
    t.start();
  }

  @Test
  public void subscribe() throws InterruptedException {
    cluster.subscribe(new JedisPubSub() {
      public void onMessage(String channel, String message) {
        assertEquals("foo", channel);
        assertEquals("exit", message);
        unsubscribe();
      }

      public void onSubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(1, subscribedChannels);

        // now that I'm subscribed... publish
        publishOne("foo", "exit");
      }

      public void onUnsubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(0, subscribedChannels);
      }
    }, "foo");
  }

  @Test
  public void rawPingBroadcast() {
    String reply = cluster.broadcastCommand(
        new CommandObject<>(new CommandArguments(Protocol.Command.PING), BuilderFactory.STRING));
    assertEquals("PONG", reply);
  }

  @Test
  public void pingBroadcast() {
    assertEquals("PONG", cluster.ping());
  }

  @Test
  public void info() {
    String info = cluster.info();
    assertThat(info, notNullValue());

    info = cluster.info("server");
    assertThat(info, notNullValue());
  }

  @Test
  public void flushAllBroadcast() {
    assertNull(cluster.get("foo"));
    assertEquals("OK", cluster.set("foo", "bar"));
    assertEquals("bar", cluster.get("foo"));
    cluster.flushAll();
    assertNull(cluster.get("foo"));
  }

  @Test
  public void scanIteration() {
    Set<String> allIn = new HashSet<>(26 * 26);
    char[] arr = new char[2];
    for (int i = 0; i < 26; i++) {
      arr[0] = (char) ('a' + i);
      for (int j = 0; j < 26; j++) {
        arr[1] = (char) ('a' + j);
        String str = new String(arr);
        cluster.incr(str);
        allIn.add(str);
      }
    }

    Set<String> allScan = new HashSet<>();
    ScanIteration scan = cluster.scanIteration(10, "*");
    while (!scan.isIterationCompleted()) {
      ScanResult<String> batch = scan.nextBatch();
      allScan.addAll(batch.getResult());
    }
    assertEquals(allIn, allScan);

    Set<String> allTypeScan = new HashSet<>();
    ScanIteration typeScan = cluster.scanIteration(10, "*", "string");
    while (!typeScan.isIterationCompleted()) {
      ScanResult<String> batch = typeScan.nextBatch();
      allTypeScan.addAll(batch.getResult());
    }
    assertEquals(allIn, allTypeScan);
  }

  @Test
  public void scanIterationCollect() {
    Set<String> allIn = new HashSet<>(26 * 26);
    char[] arr = new char[2];
    for (int i = 0; i < 26; i++) {
      arr[0] = (char) ('a' + i);
      for (int j = 0; j < 26; j++) {
        arr[1] = (char) ('a' + j);
        String str = new String(arr);
        cluster.incr(str);
        allIn.add(str);
      }
    }

    assertEquals(allIn, cluster.scanIteration(100, "*").collect(new HashSet<>(26 * 26)));
  }
}
