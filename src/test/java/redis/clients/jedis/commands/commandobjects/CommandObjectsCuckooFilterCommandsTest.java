package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;

/**
 * Tests related to <a href="https://redis.io/commands/?group=cf">Cuckoo filter</a> commands.
 */
public class CommandObjectsCuckooFilterCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsCuckooFilterCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testCuckooFilterAdd() {
    String key = "testCuckooFilter";

    String reserve = exec(commandObjects.cfReserve(key, 1000));
    assertThat(reserve, equalTo("OK"));

    Boolean add = exec(commandObjects.cfAdd(key, "apple"));
    assertThat(add, equalTo(true));

    Boolean addNx = exec(commandObjects.cfAddNx(key, "apple"));
    assertThat(addNx, equalTo(false)); // "apple" already exists, NX makes this fail

    Boolean addNx2 = exec(commandObjects.cfAddNx(key, "banana"));
    assertThat(addNx2, equalTo(true));

    Long count = exec(commandObjects.cfCount(key, "apple"));
    assertThat(count, greaterThanOrEqualTo(1L));
  }

  @Test
  public void testCuckooFilterReserveInsertAndCount() {
    String key = "testCuckooFilterAdvanced";

    CFReserveParams reserveParams = new CFReserveParams()
        .bucketSize(4).maxIterations(500).expansion(1);

    String reserve = exec(commandObjects.cfReserve(key, 5000, reserveParams));
    assertThat(reserve, equalTo("OK"));

    List<Boolean> insert = exec(commandObjects.cfInsert(
        key, "apple", "banana", "carrot", "date"));
    assertThat(insert, everyItem(equalTo(true)));

    CFInsertParams insertParams = new CFInsertParams().noCreate();

    List<Boolean> insertWithParams = exec(commandObjects.cfInsert(
        key, insertParams, "eggplant", "fig", "grape", "apple"));
    assertThat(insertWithParams, everyItem(equalTo(true)));

    Long countApple = exec(commandObjects.cfCount(key, "apple"));
    assertThat(countApple, greaterThanOrEqualTo(2L));

    Long countBanana = exec(commandObjects.cfCount(key, "banana"));
    assertThat(countBanana, greaterThanOrEqualTo(1L));

    Long countNonExisting = exec(commandObjects.cfCount(key, "watermelon"));
    assertThat(countNonExisting, equalTo(0L));
  }

  @Test
  public void testCuckooFilterInsertNx() {
    String key = "testCf";

    String[] items = { "item1", "item2", "item3" };

    CFInsertParams insertParams = new CFInsertParams().capacity(1000L).noCreate();

    List<Boolean> insertNx1 = exec(commandObjects.cfInsertNx(key, items));
    assertThat(insertNx1, not(empty()));
    assertThat(insertNx1, everyItem(equalTo(true)));

    long countAfterFirstInsert = exec(commandObjects.cfCount(key, "item1"));
    assertThat(countAfterFirstInsert, greaterThanOrEqualTo(1L));

    List<Boolean> insertNx2 = exec(commandObjects.cfInsertNx(key, insertParams, items));
    assertThat(insertNx2, not(empty()));
    assertThat(insertNx2, everyItem(equalTo(false)));

    long countAfterSecondInsert = exec(commandObjects.cfCount(key, "item1"));
    assertThat(countAfterSecondInsert, greaterThanOrEqualTo(1L)); // count should remain the same
  }

  @Test
  public void testCuckooFilterExistsAndDel() {
    String key = "testCf";
    String item = "item1";

    boolean existsBeforeInsert = exec(commandObjects.cfExists(key, item));
    assertThat(existsBeforeInsert, equalTo(false));

    Boolean add = exec(commandObjects.cfAdd(key, item));
    assertThat(add, equalTo(true));

    boolean existsAfterInsert = exec(commandObjects.cfExists(key, item));
    assertThat(existsAfterInsert, equalTo(true));

    boolean delete = exec(commandObjects.cfDel(key, item));
    assertThat(delete, equalTo(true));

    boolean existsAfterDelete = exec(commandObjects.cfExists(key, item));
    assertThat(existsAfterDelete, equalTo(false));
  }

  @Test
  public void testCuckooFilterMExists() {
    String key = "testCf";

    exec(commandObjects.cfInsert(key, "item1", "item2", "item3"));

    List<Boolean> mExists = exec(commandObjects.cfMExists(
        key, "item1", "item2", "item3", "item4", "item5"));

    assertThat(mExists, contains(true, true, true, false, false));
  }

  @Test
  public void testCuckooFilterScanDumpAndLoadChunk() {
    long capacity = 5000;

    CFReserveParams reserveParams = new CFReserveParams()
        .bucketSize(4).maxIterations(500).expansion(1);

    String key = "testCf";

    String reserve = exec(commandObjects.cfReserve(key, capacity, reserveParams));
    assertThat(reserve, equalTo("OK"));

    // add some items to the source
    for (int i = 0; i < 1000; i++) {
      exec(commandObjects.cfAdd(key, "item" + i));
    }

    String newKey = "testCfLoadChunk";

    // scandump and load
    long iterator = 0;
    do {
      Map.Entry<Long, byte[]> scanDumpResult = exec(commandObjects.cfScanDump(key, iterator));

      iterator = scanDumpResult.getKey();
      if (iterator > 0) {
        byte[] data = scanDumpResult.getValue();
        assertThat(data, notNullValue());

        String loadChunk = exec(commandObjects.cfLoadChunk(newKey, iterator, data));
        assertThat(loadChunk, equalTo("OK"));
      }
    } while (iterator != 0);

    // verify destination
    for (int i = 0; i < 1000; i++) {
      boolean exists = exec(commandObjects.cfExists(newKey, "item" + i));
      assertThat(exists, equalTo(true));
    }

    boolean missingItem = exec(commandObjects.cfExists(newKey, "item1001"));
    assertThat(missingItem, equalTo(false));
  }

  @Test
  public void testCuckooFilterInfo() {
    String key = "testCfInfo";

    exec(commandObjects.cfReserve(key, 1000));

    exec(commandObjects.cfAdd(key, "item1"));

    Map<String, Object> info = exec(commandObjects.cfInfo(key));

    assertThat(info, hasEntry("Number of items inserted", 1L));
  }
}
