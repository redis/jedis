package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;

/**
 * Tests related to <a href="https://redis.io/commands/?group=topk">Top-k</a> commands.
 */
public class CommandObjectsTopkCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsTopkCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testTopKAddAndQuery() {
    String key = "testTopK";

    long topKSize = 3;

    String reserve = exec(commandObjects.topkReserve(key, topKSize));
    assertThat(reserve, equalTo("OK"));

    List<String> add = exec(commandObjects.topkAdd(key,
        "apple", "banana", "carrot", "apple", "banana",
        "date", "eggplant", "fig", "grape", "apple"));
    // As the values are added, some items get kicked out from top 3. They are returned in the response.
    assertThat(add, contains(
        nullValue(), nullValue(), nullValue(), nullValue(), nullValue(),
        equalTo("carrot"), equalTo("date"), equalTo("eggplant"), equalTo("fig"), nullValue()
    ));

    List<Boolean> query = exec(commandObjects.topkQuery(key, "apple", "banana", "carrot", "grape"));
    assertThat(query, contains(true, true, false, true));
  }

  @Test
  public void testTopKIncrBy() {
    String key = "testTopK";

    long topKSize = 3;

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("apple", 2L);
    itemIncrements.put("banana", 3L);
    itemIncrements.put("carrot", 1L);
    itemIncrements.put("date", 5L);

    String reserve = exec(commandObjects.topkReserve(key, topKSize, 2000, 7, 0.9));
    assertThat(reserve, equalTo("OK"));

    List<String> incrBy = exec(commandObjects.topkIncrBy(key, itemIncrements));
    // Due to Map's unpredictable order, we can't assert ordering of the result
    assertThat(incrBy, hasSize(4));

    List<Boolean> query = exec(commandObjects.topkQuery(key, "apple", "banana", "date", "carrot"));
    assertThat(query, contains(true, true, true, false));
  }

  @Test
  public void testTopKListAndListWithCount() {
    String key = "testTopK";

    long topKSize = 3;

    String reserve = exec(commandObjects.topkReserve(key, topKSize));
    assertThat(reserve, equalTo("OK"));

    List<String> add = exec(commandObjects.topkAdd(key,
        "apple", "banana", "carrot", "apple", "banana",
        "date", "eggplant", "fig", "grape", "apple"));
    assertThat(add, notNullValue());

    List<String> list = exec(commandObjects.topkList(key));
    assertThat(list, contains("apple", "banana", "grape"));

    Map<String, Long> listWithCount = exec(commandObjects.topkListWithCount(key));
    assertThat(listWithCount, aMapWithSize(3));
    assertThat(listWithCount, hasEntry("apple", 3L));
    assertThat(listWithCount, hasEntry("banana", 2L));
    assertThat(listWithCount, hasEntry("grape", 1L));
  }

  @Test
  public void testTopKInfo() {
    String key = "testTopK";

    long topKSize = 3;
    long width = 1000;
    long depth = 7;
    double decay = 0.9;

    String reserve = exec(commandObjects.topkReserve(key, topKSize, width, depth, decay));
    assertThat(reserve, equalTo("OK"));

    Map<String, Object> info = exec(commandObjects.topkInfo(key));

    assertThat(info, notNullValue());
    assertThat(info, hasEntry("k", 3L));
    assertThat(info, hasEntry("width", width));
    assertThat(info, hasEntry("depth", depth));
    assertThat(info, hasKey("decay"));
  }
}
