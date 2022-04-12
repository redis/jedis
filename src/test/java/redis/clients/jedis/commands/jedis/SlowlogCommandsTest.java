package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.resps.Slowlog;
import redis.clients.jedis.util.SafeEncoder;

public class SlowlogCommandsTest extends JedisCommandsTestBase {

  private static final List<String> LOCAL_IPS = Arrays.asList("127.0.0.1", "[::1]");

  private static final String SLOWLOG_TIME_PARAM = "slowlog-log-slower-than";
  private static final String ZERO_STRING = "0";

  private String slowlogTimeValue;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    slowlogTimeValue = jedis.configGet(SLOWLOG_TIME_PARAM).get(1);
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
    jedis.set("foo", "bar");
    jedis.set("foo2", "bar2");

    List<Slowlog> reducedLog = jedis.slowlogGet(1);
    assertEquals(1, reducedLog.size());

    Slowlog log = reducedLog.get(0);
    assertTrue(log.getId() > 0);
    assertTrue(log.getTimeStamp() > 0);
    assertTrue(log.getExecutionTime() >= 0);
    assertNotNull(log.getArgs());

    List<Object> breducedLog = jedis.slowlogGetBinary(1);
    assertEquals(1, breducedLog.size());

    List<Slowlog> log1 = jedis.slowlogGet();
    List<Object> blog1 = jedis.slowlogGetBinary();

    assertNotNull(log1);
    assertNotNull(blog1);

//    assertEquals(7, jedis.slowlogLen());
    assertTrue(jedis.slowlogLen() > 5 && jedis.slowlogLen() < 12);
    assertTrue(jedis.slowlogGet().toString().contains("SLOWLOG"));
  }

  @Test
  public void slowlogObjectDetails() {
    final String clientName = "slowlog-object-client";
    jedis.clientSetname(clientName);
    jedis.slowlogReset();
    jedis.configSet(SLOWLOG_TIME_PARAM, ZERO_STRING);

    List<Slowlog> logs = jedis.slowlogGet(); // Get only 'CONFIG SET'
    assertEquals(1, logs.size());
    Slowlog log = logs.get(0);
    assertTrue(log.getId() > 0);
    assertTrue(log.getTimeStamp() > 0);
    assertTrue(log.getExecutionTime() > 0);
    assertEquals(4, log.getArgs().size());
    assertEquals(SafeEncoder.encode(Protocol.Command.CONFIG.getRaw()), log.getArgs().get(0));
    assertEquals(SafeEncoder.encode(Protocol.Keyword.SET.getRaw()), log.getArgs().get(1));
    assertEquals(SLOWLOG_TIME_PARAM, log.getArgs().get(2));
    assertEquals(ZERO_STRING, log.getArgs().get(3));
//    assertEquals("127.0.0.1", log.getClientIpPort().getHost());
    assertTrue(LOCAL_IPS.contains(log.getClientIpPort().getHost()));
    assertTrue(log.getClientIpPort().getPort() > 0);
    assertEquals(clientName, log.getClientName());
  }

  @Test
  public void slowlogBinaryDetails() {
    final byte[] clientName = SafeEncoder.encode("slowlog-binary-client");
    jedis.clientSetname(clientName);
    jedis.slowlogReset();
    jedis.configSet(SafeEncoder.encode(SLOWLOG_TIME_PARAM), SafeEncoder.encode(ZERO_STRING));

    List<Object> logs = jedis.slowlogGetBinary(); // Get only 'CONFIG SET'
    assertEquals(1, logs.size());
    List<Object> log = (List<Object>) logs.get(0);
    assertTrue((Long) log.get(0) > 0);
    assertTrue((Long) log.get(1) > 0);
    assertTrue((Long) log.get(2) > 0);
    List<Object> args = (List<Object>) log.get(3);
    assertEquals(4, args.size());
    assertArrayEquals(Protocol.Command.CONFIG.getRaw(), (byte[]) args.get(0));
    assertArrayEquals(Protocol.Keyword.SET.getRaw(), (byte[]) args.get(1));
    assertArrayEquals(SafeEncoder.encode(SLOWLOG_TIME_PARAM), (byte[]) args.get(2));
    assertArrayEquals(Protocol.toByteArray(0), (byte[]) args.get(3));
//    assertTrue(SafeEncoder.encode((byte[]) log.get(4)).startsWith("127.0.0.1:"));
    assertTrue(((byte[]) log.get(4)).length > 0);
    assertArrayEquals(clientName, (byte[]) log.get(5));
  }
}
