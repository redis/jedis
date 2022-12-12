package redis.clients.jedis.commands.jedis;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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

import org.junit.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.SafeEncoder;

public class ControlCommandsTest extends JedisCommandsTestBase {

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
    try (Jedis master = new Jedis(HostAndPorts.getRedisServers().get(0),
        DefaultJedisClientConfig.builder().password("foobared").build())) {

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
    try (Jedis slave = new Jedis(HostAndPorts.getRedisServers().get(4),
        DefaultJedisClientConfig.builder().password("foobared").build())) {

      List<Object> role = slave.role();
      assertEquals("slave", role.get(0));
      assertEquals((long) HostAndPorts.getRedisServers().get(0).getPort(), role.get(2));
      assertEquals("connected", role.get(3));
      assertTrue(role.get(4) instanceof Long);

      // binary
      List<Object> brole = slave.roleBinary();
      assertArrayEquals("slave".getBytes(), (byte[]) brole.get(0));
      assertEquals((long) HostAndPorts.getRedisServers().get(0).getPort(), brole.get(2));
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
      assertTrue(((List) role.get(1)).contains("mymaster"));

      // binary
      List<Object> brole = sentinel.roleBinary();
      assertArrayEquals("sentinel".getBytes(), (byte[]) brole.get(0));
      assertTrue(brole.get(1) instanceof List);
      AssertUtil.assertCollectionContains((List) brole.get(1), "mymaster".getBytes());
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
        Jedis j = new Jedis();
        j.auth("foobared");
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
    List<String> info = jedis.configGet("m*");
    assertNotNull(info);
    assertFalse(info.isEmpty());
    assertTrue(info.size() % 2 == 0);
    List<byte[]> infoBinary = jedis.configGet("m*".getBytes());
    assertNotNull(infoBinary);
    assertFalse(infoBinary.isEmpty());
    assertTrue(infoBinary.size() % 2 == 0);
  }

  @Test
  public void configSet() {
    List<String> info = jedis.configGet("maxmemory");
    assertEquals("maxmemory", info.get(0));
    String memory = info.get(1);
    assertEquals("OK", jedis.configSet("maxmemory", "200"));
    assertEquals("OK", jedis.configSet("maxmemory", memory));
  }

  @Test
  public void configSetBinary() {
    byte[] maxmemory = SafeEncoder.encode("maxmemory");
    List<byte[]> info = jedis.configGet(maxmemory);
    assertArrayEquals(maxmemory, info.get(0));
    byte[] memory = info.get(1);
    assertEquals("OK", jedis.configSet(maxmemory, Protocol.toByteArray(200)));
    assertEquals("OK", jedis.configSet(maxmemory, memory));
  }

  @Test
  public void configGetSetMulti() {
    String[] params = new String[]{"hash-max-listpack-entries", "set-max-intset-entries", "zset-max-listpack-entries"};
    List<String> info = jedis.configGet(params);
    assertEquals(6, info.size());
    assertEquals("OK", jedis.configSet(info.toArray(new String[6])));

    byte[][] bparams = new byte[][]{SafeEncoder.encode("hash-max-listpack-entries"),
      SafeEncoder.encode("set-max-intset-entries"), SafeEncoder.encode("zset-max-listpack-entries")};
    List<byte[]> binfo = jedis.configGet(bparams);
    assertEquals(6, binfo.size());
    assertEquals("OK", jedis.configSet(binfo.toArray(new byte[6][])));
  }

  @Test
  public void waitReplicas() {
    assertEquals(1, jedis.waitReplicas(1, 100));
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

      assertThat(latencyRead.get(), lessThan(100L));

      assertThat(latencyWrite.get(), greaterThan(100L));

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
}
