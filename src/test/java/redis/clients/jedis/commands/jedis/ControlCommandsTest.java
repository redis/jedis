package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.*;
import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.LatencyEvent;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.CommandListFilterByParams;
import redis.clients.jedis.params.LolwutParams;
import redis.clients.jedis.resps.CommandDocument;
import redis.clients.jedis.resps.CommandInfo;
import redis.clients.jedis.resps.LatencyHistoryInfo;
import redis.clients.jedis.resps.LatencyLatestInfo;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class ControlCommandsTest extends JedisCommandsTestBase {

  public ControlCommandsTest(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  @Test
  public void save() {
    try {
      String status = jedis.save();
      assertEquals("OK", status);
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgsave() {
    try {
      String status = jedis.bgsave();
      assertEquals("Background saving started", status);
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgsaveSchedule() {
    Set<String> responses = new HashSet<>();
    responses.add("OK");
    responses.add("Background saving scheduled");
    responses.add("Background saving started");

    String status = jedis.bgsaveSchedule();
    assertTrue(responses.contains(status));
  }

  @Test
  public void bgrewriteaof() {
    String scheduled = "Background append only file rewriting scheduled";
    String started = "Background append only file rewriting started";

    String status = jedis.bgrewriteaof();

    boolean ok = status.equals(scheduled) || status.equals(started);
    assertTrue(ok);
  }

  @Test
  public void lastsave() throws InterruptedException {
    long saved = jedis.lastsave();
    assertTrue(saved > 0);
  }

  @Test
  public void info() {
    String info = jedis.info();
    assertNotNull(info);
    info = jedis.info("server");
    assertNotNull(info);
  }

  @Test
  public void readonly() {
    try {
      jedis.readonly();
    } catch (JedisDataException e) {
      assertTrue("ERR This instance has cluster support disabled".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void readwrite() {
    try {
      jedis.readwrite();
    } catch (JedisDataException e) {
      assertTrue("ERR This instance has cluster support disabled".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void roleMaster() {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

    try (Jedis master = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {

      List<Object> role = master.role();
      assertEquals("master", role.get(0));
      assertTrue(role.get(1) instanceof Long);
      assertTrue(role.get(2) instanceof List);

      // binary
      List<Object> brole = master.roleBinary();
      assertArrayEquals("master".getBytes(), (byte[]) brole.get(0));
      assertTrue(brole.get(1) instanceof Long);
      assertTrue(brole.get(2) instanceof List);
    }
  }

  @Test
  public void roleSlave() {
    EndpointConfig primaryEndpoint = HostAndPorts.getRedisEndpoint("standalone0");
    EndpointConfig secondaryEndpoint = HostAndPorts.getRedisEndpoint(
        "standalone4-replica-of-standalone1");

    try (Jedis slave = new Jedis(secondaryEndpoint.getHostAndPort(),
        secondaryEndpoint.getClientConfigBuilder().build())) {

      List<Object> role = slave.role();
      assertEquals("slave", role.get(0));
      assertEquals((long) primaryEndpoint.getPort(), role.get(2));
      assertEquals("connected", role.get(3));
      assertTrue(role.get(4) instanceof Long);

      // binary
      List<Object> brole = slave.roleBinary();
      assertArrayEquals("slave".getBytes(), (byte[]) brole.get(0));
      assertEquals((long) primaryEndpoint.getPort(), brole.get(2));
      assertArrayEquals("connected".getBytes(), (byte[]) brole.get(3));
      assertTrue(brole.get(4) instanceof Long);
    }
  }

  @Test
  public void roleSentinel() {
    try (Jedis sentinel = new Jedis(HostAndPorts.getSentinelServers().get(0))) {

      List<Object> role = sentinel.role();
      assertEquals("sentinel", role.get(0));
      assertTrue(role.get(1) instanceof List);
      AssertUtil.assertCollectionContains((List) role.get(1), "mymaster");

      // binary
      List<Object> brole = sentinel.roleBinary();
      assertArrayEquals("sentinel".getBytes(), (byte[]) brole.get(0));
      assertTrue(brole.get(1) instanceof List);
      AssertUtil.assertByteArrayCollectionContains((List) brole.get(1), "mymaster".getBytes());
    }
  }

  @Test
  public void monitor() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // sleep 100ms to make sure that monitor thread runs first
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        Jedis j = new Jedis(endpoint.getHostAndPort());
        j.auth(endpoint.getPassword());
        for (int i = 0; i < 5; i++) {
          j.incr("foobared");
        }
        j.disconnect();
      }
    }).start();

    jedis.monitor(new JedisMonitor() {
      private int count = 0;

      @Override
      public void onCommand(String command) {
        if (command.contains("INCR")) {
          count++;
        }
        if (count == 5) {
          client.disconnect();
        }
      }
    });
  }

  @Test
  public void configGet() {
    Map<String, String> info = jedis.configGet("m*");
    assertNotNull(info);
    assertFalse(info.isEmpty());
//    assertTrue(info.size() % 2 == 0);
    Map<byte[], byte[]> infoBinary = jedis.configGet("m*".getBytes());
    assertNotNull(infoBinary);
    assertFalse(infoBinary.isEmpty());
//    assertTrue(infoBinary.size() % 2 == 0);
  }

  @Test
  public void configSet() {
    Map<String, String> info = jedis.configGet("maxmemory");
//    assertEquals("maxmemory", info.get(0));
//    String memory = info.get(1);
    String memory = info.get("maxmemory");
    assertNotNull(memory);
    assertEquals("OK", jedis.configSet("maxmemory", "200"));
    assertEquals("OK", jedis.configSet("maxmemory", memory));
  }

  @Test
  public void configSetBinary() {
    byte[] maxmemory = SafeEncoder.encode("maxmemory");
    Map<byte[], byte[]> info = jedis.configGet(maxmemory);
//    assertArrayEquals(maxmemory, info.get(0));
//    byte[] memory = info.get(1);
    byte[] memory = info.get(maxmemory);
    assertNotNull(memory);
    assertEquals("OK", jedis.configSet(maxmemory, Protocol.toByteArray(200)));
    assertEquals("OK", jedis.configSet(maxmemory, memory));
  }

  @Test
  public void configGetSetMulti() {
    String[] params = new String[]{"hash-max-listpack-entries", "set-max-intset-entries", "zset-max-listpack-entries"};
    Map<String, String> info = jedis.configGet(params);
    assertEquals(3, info.size());
    assertEquals("OK", jedis.configSet(info));

    byte[][] bparams = new byte[][]{SafeEncoder.encode("hash-max-listpack-entries"),
      SafeEncoder.encode("set-max-intset-entries"), SafeEncoder.encode("zset-max-listpack-entries")};
    Map<byte[], byte[]> binfo = jedis.configGet(bparams);
    assertEquals(3, binfo.size());
    assertEquals("OK", jedis.configSetBinary(binfo));
  }

  @Test
  public void waitReplicas() {
    assertEquals(1, jedis.waitReplicas(1, 100));
  }

  @Test
  public void waitAof() {
    assertEquals(KeyValue.of(0L, 0L), jedis.waitAOF(0L, 0L, 100L));
  }

  @Test
  public void clientPause() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try (Jedis jedisToPause1 = createJedis(); Jedis jedisToPause2 = createJedis();) {

      jedis.clientPause(1000L);

      Future<Long> latency1 = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          assertEquals("PONG", jedisToPause1.ping());
          return System.currentTimeMillis() - startMillis;
        }
      });
      Future<Long> latency2 = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          assertEquals("PONG", jedisToPause2.ping());
          return System.currentTimeMillis() - startMillis;
        }
      });

      assertThat(latency1.get(), greaterThan(100L));
      assertThat(latency2.get(), greaterThan(100L));

    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void clientPauseAll() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    try (Jedis jedisPause = createJedis()) {

      jedis.clientPause(1000L, ClientPauseMode.ALL);

      Future<Long> latency = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisPause.get("key");
          return System.currentTimeMillis() - startMillis;
        }
      });

      assertThat(latency.get(), greaterThan(100L));

    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void clientPauseWrite() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try (Jedis jedisRead = createJedis(); Jedis jedisWrite = createJedis();) {

      jedis.clientPause(1000L, ClientPauseMode.WRITE);

      Future<Long> latencyRead = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisRead.get("key");
          return System.currentTimeMillis() - startMillis;
        }
      });
      Future<Long> latencyWrite = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisWrite.set("key", "value");
          return System.currentTimeMillis() - startMillis;
        }
      });

      assertThat(latencyRead.get(), Matchers.lessThan(100L));

      assertThat(latencyWrite.get(), Matchers.greaterThan(100L));

    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void clientUnpause() {
    assertEquals("OK", jedis.clientUnpause());
  }

  @Test
  public void clientNoEvict() {
    assertEquals("OK", jedis.clientNoEvictOn());
    assertEquals("OK", jedis.clientNoEvictOff());
  }

  @Test
  public void clientNoTouch() {
    assertEquals("OK", jedis.clientNoTouchOn());
    assertEquals("OK", jedis.clientNoTouchOff());
  }

  @Test
  public void memoryDoctorString() {
    String memoryInfo = jedis.memoryDoctor();
    assertNotNull(memoryInfo);
  }

  @Test
  public void memoryDoctorBinary() {
    byte[] memoryInfo = jedis.memoryDoctorBinary();
    assertNotNull(memoryInfo);
  }

  @Test
  public void memoryUsageString() {
    // Note: It has been recommended not to base MEMORY USAGE test on exact value, as the response
    // may subject to be 'tuned' especially targeting a major Redis release.

    jedis.set("foo", "bar");
    assertThat(jedis.memoryUsage("foo"), greaterThan(20l));

    jedis.lpush("foobar", "fo", "ba", "sha");
    assertThat(jedis.memoryUsage("foobar", 2), greaterThan(36l));

    assertNull(jedis.memoryUsage("roo", 2));
  }

  @Test
  public void memoryUsageBinary() {
    // Note: It has been recommended not to base MEMORY USAGE test on exact value, as the response
    // may subject to be 'tuned' especially targeting a major Redis release.

    byte[] bfoo = {0x01, 0x02, 0x03, 0x04};
    byte[] bbar = {0x05, 0x06, 0x07, 0x08};
    byte[] bfoobar = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

    jedis.set(bfoo, bbar);
    assertThat(jedis.memoryUsage(bfoo), greaterThan(20l));

    jedis.lpush(bfoobar, new byte[]{0x01, 0x02}, new byte[]{0x05, 0x06}, new byte[]{0x00});
    assertThat(jedis.memoryUsage(bfoobar, 2), greaterThan(40l));

    assertNull(jedis.memoryUsage("roo", 2));
  }

  @Test
  public void memoryPurge() {
     String memoryPurge = jedis.memoryPurge();
     assertNotNull(memoryPurge);
  }

  @Test
  public void memoryStats() {
    Map<String, Object> stats = jedis.memoryStats();
    assertNotNull(stats);
  }

  @Test
  public void latencyDoctor() {
    String report = jedis.latencyDoctor();
    assertNotNull(report);
  }

  @Test
  public void latencyLatest() {
    Map<String, LatencyLatestInfo> report = jedis.latencyLatest();
    assertNotNull(report);
  }

  @Test
  public void latencyHistoryFork() {
    List<LatencyHistoryInfo> report = jedis.latencyHistory(LatencyEvent.FORK);
    assertNotNull(report);
  }

  @Test
  public void latencyReset() {
    assertTrue(jedis.latencyReset() >= 0);
  }

  @Test
  public void commandCount() {
    assertTrue(jedis.commandCount() > 100);
  }

  @Test
  public void commandDocs() {
    Map<String, CommandDocument> docs = jedis.commandDocs("SORT", "SET");

    CommandDocument sortDoc = docs.get("sort");
    assertEquals("generic", sortDoc.getGroup());
    MatcherAssert.assertThat(sortDoc.getSummary(), Matchers.isOneOf(
        "Sort the elements in a list, set or sorted set",
        "Sorts the elements in a list, a set, or a sorted set, optionally storing the result."));
    assertNull(sortDoc.getHistory());

    CommandDocument setDoc = docs.get("set");
    assertEquals("1.0.0", setDoc.getSince());
    assertEquals("O(1)", setDoc.getComplexity());
    assertEquals("2.6.12: Added the `EX`, `PX`, `NX` and `XX` options.", setDoc.getHistory().get(0));
  }

  @Test
  public void commandGetKeys() {
    List<String> keys = jedis.commandGetKeys("SORT", "mylist", "ALPHA", "STORE", "outlist");
    assertEquals(2, keys.size());

    List<KeyValue<String, List<String>>> keySandFlags = jedis.commandGetKeysAndFlags("SET", "k1", "v1");
    assertEquals("k1", keySandFlags.get(0).getKey());
    assertEquals(2, keySandFlags.get(0).getValue().size());
  }

  @Test
  public void commandInfo() {
    Map<String, CommandInfo> infos = jedis.commandInfo("GET", "foo", "SET");

    CommandInfo getInfo = infos.get("get");
    assertEquals(2, getInfo.getArity());
    assertEquals(2, getInfo.getFlags().size());
    assertEquals(1, getInfo.getFirstKey());
    assertEquals(1, getInfo.getLastKey());
    assertEquals(1, getInfo.getStep());

    assertNull(infos.get("foo")); // non-existing command

    CommandInfo setInfo = infos.get("set");
    assertEquals(3, setInfo.getAclCategories().size());
    assertEquals(0, setInfo.getTips().size());
    assertEquals(0, setInfo.getSubcommands().size());
  }

  @Test
  public void commandList() {
    List<String> commands = jedis.commandList();
    assertTrue(commands.size() > 100);

    commands = jedis.commandListFilterBy(CommandListFilterByParams.commandListFilterByParams().filterByModule("JSON"));
    assertEquals(0, commands.size()); // json module was not loaded

    commands = jedis.commandListFilterBy(CommandListFilterByParams.commandListFilterByParams().filterByAclCat("admin"));
    assertTrue(commands.size() > 10);

    commands = jedis.commandListFilterBy(CommandListFilterByParams.commandListFilterByParams().filterByPattern("a*"));
    assertTrue(commands.size() > 10);

    assertThrows(IllegalArgumentException.class, () ->
        jedis.commandListFilterBy(CommandListFilterByParams.commandListFilterByParams()));
  }

  @Test
  public void lolwut() {
    assertNotNull(jedis.lolwut());

    assertNotNull(jedis.lolwut(new LolwutParams().version(5)));

    assertNotNull(jedis.lolwut(new LolwutParams().version(5).optionalArguments()));
  }
}
