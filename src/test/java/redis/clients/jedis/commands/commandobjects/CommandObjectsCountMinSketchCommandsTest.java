package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;

/**
 * Tests related to <a href="https://redis.io/commands/?group=cms">Count-min sketch</a> commands.
 */
public class CommandObjectsCountMinSketchCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsCountMinSketchCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testIncrByAndQuery() {
    String key = "testCMS";

    String init = exec(commandObjects.cmsInitByDim(key, 10000, 5));
    assertThat(init, equalTo("OK"));

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("apple", 30L);
    itemIncrements.put("banana", 20L);
    itemIncrements.put("carrot", 10L);

    List<Long> incrBy = exec(commandObjects.cmsIncrBy(key, itemIncrements));
    // due to Map's unpredictable order, we can't assert ordering of the result
    assertThat(incrBy, containsInAnyOrder(10L, 20L, 30L));

    List<Long> query = exec(commandObjects.cmsQuery(key, "apple", "banana", "carrot", "date"));

    assertThat(query, notNullValue());
    assertThat(query.size(), equalTo(4));

    assertThat(query.get(0), greaterThanOrEqualTo(30L)); // apple
    assertThat(query.get(1), greaterThanOrEqualTo(20L)); // banana
    assertThat(query.get(2), greaterThanOrEqualTo(10L)); // carrot
    assertThat(query.get(3), lessThanOrEqualTo(1L)); // date, in practice, could be >0 due to estimation error
  }

  @Test
  public void testCMSInitByProb() {
    String key = "testCMS";

    String init = exec(commandObjects.cmsInitByProb(key, 0.01, 0.99));
    assertThat(init, equalTo("OK"));

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("apple", 5L);
    itemIncrements.put("banana", 3L);
    itemIncrements.put("carrot", 8L);

    List<Long> incrBy = exec(commandObjects.cmsIncrBy(key, itemIncrements));
    assertThat(incrBy, containsInAnyOrder(3L, 5L, 8L));

    List<Long> query = exec(commandObjects.cmsQuery(key, "apple", "banana", "carrot", "dragonfruit"));

    assertThat(query, notNullValue());
    assertThat(query.size(), equalTo(4));

    assertThat(query.get(0), greaterThanOrEqualTo(5L)); // apple
    assertThat(query.get(1), greaterThanOrEqualTo(3L)); // banana
    assertThat(query.get(2), greaterThanOrEqualTo(8L)); // carrot
    // "dragonfruit" was not incremented, its count should be minimal, but due to the probabilistic nature of CMS, it might not be exactly 0.
    assertThat(query.get(3), lessThanOrEqualTo(1L));
  }

  @Test
  public void testCMSMerge() {
    String cmsKey1 = "testCMS1";
    String cmsKey2 = "testCMS2";
    String cmsDestKey = "testCMSMerged";

    long width = 10000;
    long depth = 5;

    String init1 = exec(commandObjects.cmsInitByDim(cmsKey1, width, depth));
    assertThat(init1, equalTo("OK"));

    String init2 = exec(commandObjects.cmsInitByDim(cmsKey2, width, depth));
    assertThat(init2, equalTo("OK"));

    Map<String, Long> itemIncrements1 = new HashMap<>();
    itemIncrements1.put("apple", 2L);
    itemIncrements1.put("banana", 3L);

    List<Long> incrBy1 = exec(commandObjects.cmsIncrBy(cmsKey1, itemIncrements1));
    assertThat(incrBy1, containsInAnyOrder(2L, 3L));

    Map<String, Long> itemIncrements2 = new HashMap<>();
    itemIncrements2.put("carrot", 5L);
    itemIncrements2.put("date", 4L);

    List<Long> incrBy2 = exec(commandObjects.cmsIncrBy(cmsKey2, itemIncrements2));
    assertThat(incrBy2, containsInAnyOrder(4L, 5L));

    String init3 = exec(commandObjects.cmsInitByDim(cmsDestKey, width, depth));
    assertThat(init3, equalTo("OK"));

    String merge = exec(commandObjects.cmsMerge(cmsDestKey, cmsKey1, cmsKey2));
    assertThat(merge, equalTo("OK"));

    List<Long> query = exec(commandObjects.cmsQuery(cmsDestKey, "apple", "banana", "carrot", "date"));

    assertThat(query, notNullValue());
    assertThat(query.size(), equalTo(4));

    assertThat(query.get(0), greaterThanOrEqualTo(2L)); // apple
    assertThat(query.get(1), greaterThanOrEqualTo(3L)); // banana
    assertThat(query.get(2), greaterThanOrEqualTo(5L)); // carrot
    assertThat(query.get(3), greaterThanOrEqualTo(4L)); // date
  }

  @Test
  public void testCMSMergeWithWeights() {
    String cmsKey1 = "testCMS1";
    String cmsKey2 = "testCMS2";
    String cmsDestKey = "testCMSMerged";

    long width = 10000;
    long depth = 5;

    String init1 = exec(commandObjects.cmsInitByDim(cmsKey1, width, depth));
    assertThat(init1, equalTo("OK"));

    String init2 = exec(commandObjects.cmsInitByDim(cmsKey2, width, depth));
    assertThat(init2, equalTo("OK"));

    Map<String, Long> itemIncrements1 = new HashMap<>();
    itemIncrements1.put("apple", 2L);
    itemIncrements1.put("banana", 3L);

    List<Long> incrBy1 = exec(commandObjects.cmsIncrBy(cmsKey1, itemIncrements1));
    assertThat(incrBy1, containsInAnyOrder(2L, 3L));

    Map<String, Long> itemIncrements2 = new HashMap<>();
    itemIncrements2.put("carrot", 5L);
    itemIncrements2.put("date", 4L);

    List<Long> incrBy2 = exec(commandObjects.cmsIncrBy(cmsKey2, itemIncrements2));
    assertThat(incrBy2, containsInAnyOrder(4L, 5L));

    String init3 = exec(commandObjects.cmsInitByDim(cmsDestKey, width, depth));
    assertThat(init3, equalTo("OK"));

    // Weights for the CMS keys to be merged
    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put(cmsKey1, 1L);
    keysAndWeights.put(cmsKey2, 2L);

    String merge = exec(commandObjects.cmsMerge(cmsDestKey, keysAndWeights));
    assertThat(merge, equalTo("OK"));

    List<Long> query = exec(commandObjects.cmsQuery(cmsDestKey, "apple", "banana", "carrot", "date"));

    assertThat(query, notNullValue());
    assertThat(query.size(), equalTo(4));

    assertThat(query.get(0), greaterThanOrEqualTo(2L)); // apple, weight of 1
    assertThat(query.get(1), greaterThanOrEqualTo(3L)); // banana, weight of 1
    assertThat(query.get(2), greaterThanOrEqualTo(10L)); // carrot, weight of 2, so 5 * 2
    assertThat(query.get(3), greaterThanOrEqualTo(8L)); // date, weight of 2, so 4 * 2
  }

  @Test
  public void testCMSInfo() {
    String key = "testCMS";

    long width = 10000;
    long depth = 5;

    String init = exec(commandObjects.cmsInitByDim(key, width, depth));
    assertThat(init, equalTo("OK"));

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("apple", 3L);
    itemIncrements.put("banana", 2L);
    itemIncrements.put("carrot", 1L);

    List<Long> incrBy = exec(commandObjects.cmsIncrBy(key, itemIncrements));
    assertThat(incrBy, hasSize(3));

    Map<String, Object> info = exec(commandObjects.cmsInfo(key));

    assertThat(info, hasEntry("width", 10000L));
    assertThat(info, hasEntry("depth", 5L));
    assertThat(info, hasEntry("count", 6L)); // 3 + 2 + 1
  }
}
