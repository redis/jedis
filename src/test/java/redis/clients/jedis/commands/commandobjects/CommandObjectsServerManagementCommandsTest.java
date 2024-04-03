package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.Test;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.RedisProtocol;

/**
 * Tests related to <a href="https://redis.io/commands/?group=server">Server management</a> commands.
 */
public class CommandObjectsServerManagementCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsServerManagementCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testSlowLogReset() {
    String reset = exec(commandObjects.slowlogReset());
    assertThat(reset, equalTo("OK"));
  }

  @Test
  public void testMemoryUsage() {
    String key = "key";
    int samples = 5;

    exec(commandObjects.set(key, "value"));

    CommandObject<Long> memoryUsage = commandObjects.memoryUsage(key);
    assertThat(exec(memoryUsage), greaterThan(0L));

    CommandObject<Long> memoryUsageWithSamples = commandObjects.memoryUsage(key, samples);
    assertThat(exec(memoryUsageWithSamples), greaterThan(0L));

    CommandObject<Long> memoryUsageBinary = commandObjects.memoryUsage(key.getBytes());
    assertThat(exec(memoryUsageBinary), greaterThan(0L));

    CommandObject<Long> memoryUsageBinaryWithSamples = commandObjects.memoryUsage(key.getBytes(), samples);
    assertThat(exec(memoryUsageBinaryWithSamples), greaterThan(0L));
  }

  @Test
  public void testObjectRefcount() {
    String key = "refcountKey";

    exec(commandObjects.set(key, "value"));

    Long refcount = exec(commandObjects.objectRefcount(key));

    assertThat(refcount, greaterThanOrEqualTo(1L));

    Long refcountBinary = exec(commandObjects.objectRefcount(key.getBytes()));

    assertThat(refcountBinary, greaterThanOrEqualTo(1L));
  }

  @Test
  public void testObjectEncoding() {
    exec(commandObjects.lpush("lst", "Hello, Redis!"));

    String encoding = exec(commandObjects.objectEncoding("lst"));

    assertThat(encoding, containsString("list"));

    byte[] encodingBinary = exec(commandObjects.objectEncoding("lst".getBytes()));

    assertThat(new String(encodingBinary), containsString("list"));
  }

  @Test
  public void testObjectIdletime() throws InterruptedException {
    String key = "idleTestString";
    String value = "Idle value test";

    exec(commandObjects.set(key, value));

    // A small delay to simulate idle time
    Thread.sleep(1000);

    Long idleTime = exec(commandObjects.objectIdletime(key));
    assertThat(idleTime, greaterThan(0L));

    Long idleTimeBinary = exec(commandObjects.objectIdletime(key.getBytes()));
    assertThat(idleTimeBinary, greaterThan(0L));
  }
}
