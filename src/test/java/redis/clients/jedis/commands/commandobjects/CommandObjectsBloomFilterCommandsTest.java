package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;

/**
 * Tests related to <a href="https://redis.io/commands/?group=bf">Bloom Filter</a> commands.
 */
public class CommandObjectsBloomFilterCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsBloomFilterCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testBfAddAndExists() {
    String key = "testBf";

    String reserve = exec(commandObjects.bfReserve(key, 0.01, 1000));
    assertThat(reserve, equalTo("OK"));

    boolean add = exec(commandObjects.bfAdd(key, "item1"));
    assertThat(add, equalTo(true));

    boolean exists = exec(commandObjects.bfExists(key, "item1"));
    assertThat(exists, equalTo(true));

    boolean notExists = exec(commandObjects.bfExists(key, "item2"));
    assertThat(notExists, equalTo(false));
  }

  @Test
  public void testBfInsert() {
    String key = "testBf";

    String reserve = exec(commandObjects.bfReserve(key, 0.01, 1000));
    assertThat(reserve, equalTo("OK"));

    List<Boolean> insert = exec(commandObjects.bfInsert(key, "item1", "item2"));
    assertThat(insert, contains(true, true));

    BFInsertParams insertParams = new BFInsertParams().noCreate().capacity(1000);

    List<Boolean> insertWithParams = exec(commandObjects.bfInsert(key, insertParams, "item1", "item2"));
    assertThat(insertWithParams, contains(false, false));

    assertThat(exec(commandObjects.bfExists(key, "item1")), equalTo(true));
    assertThat(exec(commandObjects.bfExists(key, "item2")), equalTo(true));
    assertThat(exec(commandObjects.bfExists(key, "item3")), equalTo(false));
  }

  @Test
  public void testBfMAddMExistsAndCard() {
    String key = "testBf";

    String reserve = exec(commandObjects.bfReserve(key, 0.01, 1000));
    assertThat(reserve, equalTo("OK"));

    List<Boolean> mAdd = exec(commandObjects.bfMAdd(key, "item1", "item2", "item3"));
    assertThat(mAdd, contains(true, true, true));

    List<Boolean> mExists = exec(commandObjects.bfMExists(key, "item1", "item2", "item3", "item4"));
    assertThat(mExists, contains(true, true, true, false));

    Long card = exec(commandObjects.bfCard(key));
    assertThat(card, equalTo(3L));
  }

  @Test
  public void testBfScanDumpAndLoadChunk() {
    String key = "test";

    String reserve = exec(commandObjects.bfReserve(key, 0.01, 5000));
    assertThat(reserve, equalTo("OK"));

    for (int i = 0; i < 1000; i++) {
      Boolean add = exec(commandObjects.bfAdd(key, "item" + i));
      assertThat(add, equalTo(true));
    }

    String newKey = "testBfLoadChunk";

    long iterator = 0;
    do {
      Map.Entry<Long, byte[]> scanDumpResult = exec(commandObjects.bfScanDump(key, iterator));

      iterator = scanDumpResult.getKey();

      if (iterator > 0) {
        byte[] data = scanDumpResult.getValue();

        assertThat(data, notNullValue());

        String loadChunk = exec(commandObjects.bfLoadChunk(newKey, iterator, data));
        assertThat(loadChunk, equalTo("OK"));
      }
    } while (iterator != 0);

    // verify destination
    for (int i = 0; i < 1000; i++) {
      Boolean exists = exec(commandObjects.bfExists(newKey, "item" + i));
      assertThat(exists, equalTo(true));
    }

    Boolean missingItem = exec(commandObjects.bfExists(newKey, "item1001"));
    assertThat(missingItem, equalTo(false));
  }

  @Test
  public void testBfInfo() {
    String key = "testBf";

    double errorRate = 0.01;
    long capacity = 1000;
    BFReserveParams reserveParams = new BFReserveParams().expansion(2);

    String reserve = exec(commandObjects.bfReserve(key, errorRate, capacity, reserveParams));
    assertThat(reserve, equalTo("OK"));

    Boolean add = exec(commandObjects.bfAdd(key, "item1"));
    assertThat(add, equalTo(true));

    Map<String, Object> info = exec(commandObjects.bfInfo(key));
    assertThat(info, hasEntry("Capacity", 1000L));
    assertThat(info, hasEntry("Number of items inserted", 1L));
  }
}
