package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.resps.Slowlog;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class SlowlogCommandsTest extends JedisCommandsTestBase {

  private static final String SLOWLOG_TIME_PARAM = "slowlog-log-slower-than";
  private static final String ZERO_STRING = "0";

  private String slowlogTimeValue;

  public SlowlogCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    slowlogTimeValue = jedis.configGet(SLOWLOG_TIME_PARAM).get(SLOWLOG_TIME_PARAM);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    jedis.configSet(SLOWLOG_TIME_PARAM, slowlogTimeValue);
    super.tearDown();
  }

  @Test
  public void slowlog() {
    jedis.configSet(SLOWLOG_TIME_PARAM, ZERO_STRING);
    jedis.slowlogReset();

    jedis.set("foo", "bar");
    jedis.set("foo2", "bar2");

    List<Slowlog> reducedLog = jedis.slowlogGet(1);
    assertEquals(1, reducedLog.size());

    Slowlog log = reducedLog.get(0);
    assertThat(log.getId(), Matchers.greaterThan(0L));
    assertThat(log.getTimeStamp(), Matchers.greaterThan(0L));
    assertThat(log.getExecutionTime(), Matchers.greaterThanOrEqualTo(0L));
    assertNotNull(log.getArgs());

    List<Object> breducedLog = jedis.slowlogGetBinary(1);
    assertEquals(1, breducedLog.size());

    List<Slowlog> log1 = jedis.slowlogGet();
    List<Object> blog1 = jedis.slowlogGetBinary();

    assertNotNull(log1);
    assertNotNull(blog1);

//    assertEquals(7, jedis.slowlogLen());
    assertThat(jedis.slowlogLen(),
        Matchers.allOf(Matchers.greaterThanOrEqualTo(6L), Matchers.lessThanOrEqualTo(13L)));
    assertThat(jedis.slowlogGet().toString(), Matchers.containsString("SLOWLOG"));
  }

  @Test
  public void slowlogObjectDetails() {
    final String clientName = "slowlog-object-client-" + UUID.randomUUID();
    jedis.clientSetname(clientName);
    jedis.slowlogReset();
    jedis.configSet(SLOWLOG_TIME_PARAM, ZERO_STRING);

    List<Slowlog> logs = jedis.slowlogGet(); // Get only 'CONFIG SET', or including 'SLOWLOG RESET'
    //assertEquals(1, logs.size());
    assertThat(logs.size(), Matchers.allOf(Matchers.greaterThanOrEqualTo(1), Matchers.lessThanOrEqualTo(2)));
    Slowlog log = logs.get(0);
    assertEquals(clientName, log.getClientName());
    assertThat(log.getId(), Matchers.greaterThan(0L));
    assertThat(log.getTimeStamp(), Matchers.greaterThan(0L));
    assertThat(log.getExecutionTime(), Matchers.greaterThanOrEqualTo(0L));
    assertEquals(4, log.getArgs().size());
    assertEquals(SafeEncoder.encode(Protocol.Command.CONFIG.getRaw()), log.getArgs().get(0));
    assertEquals(SafeEncoder.encode(Protocol.Keyword.SET.getRaw()), log.getArgs().get(1));
    assertEquals(SLOWLOG_TIME_PARAM, log.getArgs().get(2));
    assertEquals(ZERO_STRING, log.getArgs().get(3));
    assertThat(log.getClientIpPort().getHost(), Matchers.in(getAllLocalIps()));
    assertThat(log.getClientIpPort().getPort(), Matchers.greaterThan(0));
  }

  @Test
  public void slowlogBinaryObjectDetails() {
    final byte[] clientName = SafeEncoder.encode("slowlog-binary-client");
    jedis.clientSetname(clientName);
    jedis.slowlogReset();
    jedis.configSet(SafeEncoder.encode(SLOWLOG_TIME_PARAM), SafeEncoder.encode(ZERO_STRING));

    List<Object> logs = jedis.slowlogGetBinary(); // Get only 'CONFIG SET', or including 'SLOWLOG RESET'
    //assertEquals(1, logs.size());
    assertThat(logs.size(), Matchers.allOf(Matchers.greaterThanOrEqualTo(1), Matchers.lessThanOrEqualTo(2)));
    List<Object> log = (List<Object>) logs.get(0);
    assertThat((Long) log.get(0), Matchers.greaterThan(0L));
    assertThat((Long) log.get(1), Matchers.greaterThan(0L));
    assertThat((Long) log.get(2), Matchers.greaterThanOrEqualTo(0L));
    List<Object> args = (List<Object>) log.get(3);
    assertEquals(4, args.size());
    assertArrayEquals(Protocol.Command.CONFIG.getRaw(), (byte[]) args.get(0));
    assertArrayEquals(Protocol.Keyword.SET.getRaw(), (byte[]) args.get(1));
    assertArrayEquals(SafeEncoder.encode(SLOWLOG_TIME_PARAM), (byte[]) args.get(2));
    assertArrayEquals(Protocol.toByteArray(0), (byte[]) args.get(3));
//    assertTrue(SafeEncoder.encode((byte[]) log.get(4)).startsWith("127.0.0.1:"));
    assertThat(((byte[]) log.get(4)).length, Matchers.greaterThanOrEqualTo(10)); // 'IP:PORT'
    assertArrayEquals(clientName, (byte[]) log.get(5));
  }

  private static Set<String> getAllLocalIps()  {
    Set<String> allLocalIps = new HashSet<>();
      try {
          for (NetworkInterface netIf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress addr : Collections.list(netIf.getInetAddresses())) {
              allLocalIps.add(addr.getHostAddress());
            }
          }
      } catch (SocketException e) {
          throw new RuntimeException(e);
      }
      //ipv6 loopback
      allLocalIps.add("[::1]");
      return allLocalIps;
  }
}
