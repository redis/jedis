package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;

/**
 * Tests related to <a href="https://redis.io/commands/?group=hyperloglog">HyperLogLog</a> commands.
 */
public class CommandObjectsHyperloglogCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsHyperloglogCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testPfaddAndCount() {
    String key = "hyperloglogKey";

    Long add = exec(commandObjects.pfadd(key, "element1", "element2", "element3"));
    assertThat(add, equalTo(1L));

    Long count = exec(commandObjects.pfcount(key));
    assertThat(count, greaterThanOrEqualTo(3L)); // approximate, expect at least 3

    Long addNewElement = exec(commandObjects.pfadd(key, "element4"));
    assertThat(addNewElement, equalTo(1L));

    Long countWithNewElement = exec(commandObjects.pfcount(key));
    assertThat(countWithNewElement, greaterThan(count));
  }

  @Test
  public void testPfaddAndCountBinary() {
    byte[] key = "hyperloglogKey".getBytes();

    Long add = exec(commandObjects.pfadd(key, "element1".getBytes(), "element2".getBytes(), "element3".getBytes()));
    assertThat(add, equalTo(1L));

    Long count = exec(commandObjects.pfcount(key));
    assertThat(count, greaterThanOrEqualTo(3L));
  }

  @Test
  public void testPfmerge() {
    String key1 = "hyperloglog1";
    String key2 = "hyperloglog2";

    exec(commandObjects.pfadd(key1, "elementA", "elementB"));
    exec(commandObjects.pfadd(key2, "elementC", "elementD"));

    String destKey = "mergedHyperloglog";
    byte[] destKeyBytes = "mergedHyperloglogBytes".getBytes();

    String mergeResultWithString = exec(commandObjects.pfmerge(destKey, key1, key2));
    assertThat(mergeResultWithString, equalTo("OK"));

    Long countAfterMergeWithString = exec(commandObjects.pfcount(destKey));
    assertThat(countAfterMergeWithString, greaterThanOrEqualTo(4L));

    // binary
    String mergeResultWithBytes = exec(commandObjects.pfmerge(destKeyBytes, key1.getBytes(), key2.getBytes()));
    assertThat(mergeResultWithBytes, equalTo("OK"));

    Long countAfterMergeWithBytes = exec(commandObjects.pfcount(destKeyBytes));
    assertThat(countAfterMergeWithBytes, greaterThanOrEqualTo(4L));
  }

  @Test
  public void testPfcount() {
    String key1 = "hyperloglogCount1";
    String key2 = "hyperloglogCount2";

    exec(commandObjects.pfadd(key1, "element1", "element2", "element3"));
    exec(commandObjects.pfadd(key2, "element4", "element5", "element6"));

    Long countForKey1 = exec(commandObjects.pfcount(key1));
    assertThat(countForKey1, greaterThanOrEqualTo(3L));

    Long countForBothKeys = exec(commandObjects.pfcount(key1, key2));
    assertThat(countForBothKeys, greaterThanOrEqualTo(6L));

    // binary
    Long countForKey1Binary = exec(commandObjects.pfcount(key1.getBytes()));
    assertThat(countForKey1Binary, greaterThanOrEqualTo(3L));

    Long countForBothKeysBinary = exec(commandObjects.pfcount(key1.getBytes(), key2.getBytes()));
    assertThat(countForBothKeysBinary, greaterThanOrEqualTo(6L));
  }
}
