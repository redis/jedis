package redis.clients.jedis.commands.commandobjects;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Tests related to <a href="https://redis.io/commands/?group=hash">Hash</a> commands.
 */
public class CommandObjectsHashCommandsTest extends CommandObjectsStandaloneTestBase {

  private final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  private final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  private final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  private final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  private final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };

  public CommandObjectsHashCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testHashSetGet() {
    String key = "hashKey";
    String field = "name";
    String value = "John";

    String getInitial = exec(commandObjects.hget(key, field));
    assertThat(getInitial, nullValue());

    Long set = exec(commandObjects.hset(key, field, value));
    assertThat(set, equalTo(1L));

    String get = exec(commandObjects.hget(key, field));
    assertThat(get, equalTo(value));
  }

  @Test
  public void testHashSetGetBinary() {
    byte[] key = "hashKeyBytes".getBytes();
    byte[] field = "field".getBytes();
    byte[] value = "value".getBytes();

    byte[] getInitial = exec(commandObjects.hget(key, field));
    assertThat(getInitial, nullValue());

    Long set = exec(commandObjects.hset(key, field, value));
    assertThat(set, equalTo(1L));

    byte[] get = exec(commandObjects.hget(key, field));
    assertThat(get, equalTo(value));
  }

  @Test
  public void testHashBulkSet() {
    String key = "hashKey";

    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    Long set = exec(commandObjects.hset(key, hash));
    assertThat(set, equalTo((long) hash.size()));

    List<String> mget = exec(commandObjects.hmget(key, "field1", "field2"));
    assertThat(mget, contains("value1", "value2"));
  }

  @Test
  public void testHashBulkSetBinary() {
    byte[] key = "hashKey".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());
    hash.put("field2".getBytes(), "value2".getBytes());

    Long set = exec(commandObjects.hset(key, hash));
    assertThat(set, equalTo((long) hash.size()));

    List<byte[]> mget = exec(commandObjects.hmget(key, "field1".getBytes(), "field2".getBytes()));
    assertThat(mget, contains("value1".getBytes(), "value2".getBytes()));
  }

  @Test
  public void testHashMsetMget() {
    String key = "bulkHashKey";

    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    String mset = exec(commandObjects.hmset(key, hash));
    assertThat(mset, equalTo("OK"));

    List<String> mget = exec(commandObjects.hmget(key, "field1", "field2"));
    assertThat(mget, contains("value1", "value2"));
  }

  @Test
  public void testHashMsetMgetBinary() {
    byte[] key = "hashKey".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());
    hash.put("field2".getBytes(), "value2".getBytes());

    String mset = exec(commandObjects.hmset(key, hash));
    assertThat(mset, equalTo("OK"));

    List<byte[]> mget = exec(commandObjects.hmget(key, "field1".getBytes(), "field2".getBytes()));
    assertThat(mget, contains("value1".getBytes(), "value2".getBytes()));
  }

  @Test
  public void testHsetnx() {
    String key = "hashKey";
    String field = "field";
    String value = "value";

    String initialGet = exec(commandObjects.hget(key, field));
    assertThat(initialGet, nullValue());

    Long initialSet = exec(commandObjects.hsetnx(key, field, value));
    assertThat(initialSet, equalTo(1L));

    String get = exec(commandObjects.hget(key, field));
    assertThat(get, equalTo(value));

    Long secondSet = exec(commandObjects.hsetnx(key, field, "newValue"));
    assertThat(secondSet, equalTo(0L));

    String secondGet = exec(commandObjects.hget(key, field));
    assertThat(secondGet, equalTo(value));
  }

  @Test
  public void testHsetnxBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field".getBytes();
    byte[] value = "value".getBytes();

    byte[] initialGet = exec(commandObjects.hget(key, field));
    assertThat(initialGet, nullValue());

    Long set = exec(commandObjects.hsetnx(key, field, value));
    assertThat(set, equalTo(1L));

    byte[] get = exec(commandObjects.hget(key, field));
    assertThat(get, equalTo(value));

    Long secondSet = exec(commandObjects.hsetnx(key, field, "newValue".getBytes()));
    assertThat(secondSet, equalTo(0L));

    byte[] secondGet = exec(commandObjects.hget(key, field));
    assertThat(secondGet, equalTo(value));
  }

  @Test
  public void testHincrBy() {
    String key = "incrementHashKey";
    String field = "incrementField";

    Long initialSet = exec(commandObjects.hset(key, field, "0"));
    assertThat(initialSet, equalTo(1L));

    String initialGet = exec(commandObjects.hget(key, field));
    assertThat(initialGet, equalTo("0"));

    Long incrByLong = exec(commandObjects.hincrBy(key, field, 10L));
    assertThat(incrByLong, equalTo(10L));

    String getAfterIncrByLong = exec(commandObjects.hget(key, field));
    assertThat(getAfterIncrByLong, equalTo("10"));

    Double incrByFloat = exec(commandObjects.hincrByFloat(key, field, 2.5));
    assertThat(incrByFloat, equalTo(12.5));

    String getAfterIncrByFloat = exec(commandObjects.hget(key, field));
    assertThat(getAfterIncrByFloat, equalTo("12.5"));
  }

  @Test
  public void testHincrByBinary() {
    byte[] key = "key".getBytes();
    byte[] field = "field".getBytes();

    Long initialSet = exec(commandObjects.hset(key, field, "0".getBytes()));
    assertThat(initialSet, equalTo(1L));

    byte[] initialGet = exec(commandObjects.hget(key, field));
    assertThat(initialGet, equalTo("0".getBytes()));

    Long incrByLong = exec(commandObjects.hincrBy(key, field, 10L));
    assertThat(incrByLong, equalTo(10L));

    byte[] getAfterIncrByLong = exec(commandObjects.hget(key, field));
    assertThat(getAfterIncrByLong, equalTo("10".getBytes()));

    Double incrByDouble = exec(commandObjects.hincrByFloat(key, field, 2.5));
    assertThat(incrByDouble, equalTo(12.5));

    byte[] getAfterIncrByDouble = exec(commandObjects.hget(key, field));
    assertThat(getAfterIncrByDouble, equalTo("12.5".getBytes()));
  }

  @Test
  public void testHashExistsDel() {
    String key = "key";
    String field1 = "field1";
    String field2 = "field2";
    String value = "value";

    exec(commandObjects.hset(key, field1, value));
    exec(commandObjects.hset(key, field2, value));

    Boolean exists = exec(commandObjects.hexists(key, field1));
    assertThat(exists, equalTo(true));

    Long len = exec(commandObjects.hlen(key));
    assertThat(len, equalTo(2L));

    Long del = exec(commandObjects.hdel(key, field1));
    assertThat(del, equalTo(1L));

    Boolean existsAfterDel = exec(commandObjects.hexists(key, field1));
    assertThat(existsAfterDel, equalTo(false));

    Long lenAfterDel = exec(commandObjects.hlen(key));
    assertThat(lenAfterDel, equalTo(1L));
  }

  @Test
  public void testHashExistsDelBinary() {
    byte[] key = "key".getBytes();
    byte[] field1 = "field1".getBytes();
    byte[] field2 = "field2".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.hset(key, field1, value));
    exec(commandObjects.hset(key, field2, value));

    Boolean exists = exec(commandObjects.hexists(key, field1));
    assertThat(exists, equalTo(true));

    Long len = exec(commandObjects.hlen(key));
    assertThat(len, equalTo(2L));

    Long del = exec(commandObjects.hdel(key, field1));
    assertThat(del, equalTo(1L));

    Boolean existsAfterDel = exec(commandObjects.hexists(key, field1));
    assertThat(existsAfterDel, equalTo(false));

    Long lenAfterDel = exec(commandObjects.hlen(key));
    assertThat(lenAfterDel, equalTo(1L));
  }

  @Test
  public void testHashKeysValsGetAll() {
    String key = "hashKey";
    byte[] keyBinary = key.getBytes();

    String field1 = "field1";
    String field2 = "field2";
    String value1 = "value1";
    String value2 = "value2";

    exec(commandObjects.hset(key, field1, value1));
    exec(commandObjects.hset(key, field2, value2));

    Set<String> keys = exec(commandObjects.hkeys(key));
    assertThat(keys, containsInAnyOrder(field1, field2));

    List<String> values = exec(commandObjects.hvals(key));
    assertThat(values, containsInAnyOrder(value1, value2));

    Map<String, String> hash = exec(commandObjects.hgetAll(key));
    assertThat(hash, allOf(
        hasEntry(field1, value1),
        hasEntry(field2, value2)));

    // binary
    Set<byte[]> keysBinary = exec(commandObjects.hkeys(keyBinary));
    assertThat(keysBinary, containsInAnyOrder(field1.getBytes(), field2.getBytes()));

    List<byte[]> valuesBinary = exec(commandObjects.hvals(keyBinary));
    assertThat(valuesBinary, containsInAnyOrder(value1.getBytes(), value2.getBytes()));

    Map<byte[], byte[]> hashBinary = exec(commandObjects.hgetAll(keyBinary));
    assertThat(hashBinary, allOf(
        hasEntry(field1.getBytes(), value1.getBytes()),
        hasEntry(field2.getBytes(), value2.getBytes())));
  }

  @Test
  public void testHashRandfield() {
    String key = "testHash";
    byte[] bkey = key.getBytes();

    exec(commandObjects.hset(key, "field1", "value1"));
    exec(commandObjects.hset(key, "field2", "value2"));

    String singleField = exec(commandObjects.hrandfield(key));
    assertThat(singleField, anyOf(equalTo("field1"), equalTo("field2")));

    List<String> fields = exec(commandObjects.hrandfield(key, 2));
    assertThat(fields, containsInAnyOrder("field1", "field2"));

    List<Map.Entry<String, String>> fieldsWithValues = exec(commandObjects.hrandfieldWithValues(key, 2));

    assertThat(fieldsWithValues, hasSize(2));
    fieldsWithValues.forEach(entry ->
        assertThat(entry.getValue(), anyOf(equalTo("value1"), equalTo("value2"))));

    // binary
    byte[] singleFieldBinary = exec(commandObjects.hrandfield(bkey));
    assertThat(singleFieldBinary, anyOf(equalTo("field1".getBytes()), equalTo("field2".getBytes())));

    List<byte[]> fieldsBinary = exec(commandObjects.hrandfield(bkey, 2));
    assertThat(fieldsBinary, containsInAnyOrder("field1".getBytes(), "field2".getBytes()));

    List<Map.Entry<byte[], byte[]>> fieldsWithValuesBinary = exec(commandObjects.hrandfieldWithValues(bkey, 2));

    assertThat(fieldsWithValuesBinary, hasSize(2));
    fieldsWithValuesBinary.forEach(entry ->
        assertThat(entry.getValue(), anyOf(equalTo("value1".getBytes()), equalTo("value2".getBytes()))));
  }

  @Test
  public void testHscan() {
    String key = "testHashScan";
    byte[] bkey = key.getBytes();

    exec(commandObjects.hset(key, "field1", "value1"));
    exec(commandObjects.hset(key, "field2", "value2"));

    ScanParams params = new ScanParams().count(2);

    ScanResult<Map.Entry<String, String>> scanResult = exec(commandObjects.hscan(key, ScanParams.SCAN_POINTER_START, params));

    assertThat(scanResult.getResult(), hasSize(lessThanOrEqualTo(2)));

    scanResult.getResult().forEach(entry ->
        assertThat(entry.getKey(), anyOf(equalTo("field1"), equalTo("field2"))));
    scanResult.getResult().forEach(entry ->
        assertThat(entry.getValue(), anyOf(equalTo("value1"), equalTo("value2"))));

    ScanResult<String> scanResultNoValues = exec(commandObjects.hscanNoValues(key, ScanParams.SCAN_POINTER_START, params));

    assertThat(scanResultNoValues.getResult(), hasSize(lessThanOrEqualTo(2)));

    assertThat(scanResultNoValues.getResult(),
        everyItem(anyOf(equalTo("field1"), equalTo("field2"))));

    // binary
    ScanResult<Map.Entry<byte[], byte[]>> bscanResult = exec(commandObjects.hscan(bkey, ScanParams.SCAN_POINTER_START_BINARY, params));

    assertThat(bscanResult.getResult(), hasSize(lessThanOrEqualTo(2)));

    bscanResult.getResult().forEach(entry ->
        assertThat(entry.getKey(), anyOf(equalTo("field1".getBytes()), equalTo("field2".getBytes()))));
    bscanResult.getResult().forEach(entry ->
        assertThat(entry.getValue(), anyOf(equalTo("value1".getBytes()), equalTo("value2".getBytes()))));

    ScanResult<byte[]> bscanResultNoValues = exec(commandObjects.hscanNoValues(bkey, ScanParams.SCAN_POINTER_START_BINARY, params));

    assertThat(bscanResultNoValues.getResult(), hasSize(lessThanOrEqualTo(2)));

    assertThat(bscanResultNoValues.getResult(),
        everyItem(anyOf(equalTo("field1".getBytes()), equalTo("field2".getBytes()))));
  }

  @Test
  public void testHashStrlen() {
    String key = "testHashStrlen";
    byte[] bkey = key.getBytes();

    exec(commandObjects.hset(key, "field1", "value1"));

    Long strlen = exec(commandObjects.hstrlen(key, "field1"));
    assertThat(strlen, equalTo(6L));

    Long strlenNonExistingField = exec(commandObjects.hstrlen(key, "nonExistingField"));
    assertThat(strlenNonExistingField, equalTo(0L));

    // binary
    Long strlenBinary = exec(commandObjects.hstrlen(bkey, "field1".getBytes()));
    assertThat(strlenBinary, equalTo(6L));

    Long strlenNonExistingFieldBinary = exec(commandObjects.hstrlen(bkey, "nonExistingField".getBytes()));
    assertThat(strlenNonExistingFieldBinary, equalTo(0L));
  }

  @Test
  public void hexpireAndHttl() {
    long seconds1 = 20;
    long seconds2 = 10;

    exec(commandObjects.hset("foo", "bar", "car"));
    exec(commandObjects.hset("foo", "bare", "care"));
    assertThat(exec(commandObjects.hexpire("foo", seconds1, "bar", "bared")), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset("foo", "bared", "cared"));
    assertThat(exec(commandObjects.hexpire("foo", seconds2, ExpiryOption.NX, "bar", "bared")), equalTo(asList(0L, 1L)));

    assertThat(exec(commandObjects.httl("foo", "bar", "bare", "bared")),
        contains(greaterThanOrEqualTo(seconds1 - 1), equalTo(-1L),
            both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1))));
  }

  @Test
  public void hexpireAndHttlBinary() {
    long seconds1 = 20;
    long seconds2 = 10;

    exec(commandObjects.hset(bfoo, bbar1, bcar));
    exec(commandObjects.hset(bfoo, bbar2, bcar));
    assertThat(exec(commandObjects.hexpire(bfoo, seconds1, bbar1, bbar3)), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset(bfoo, bbar3, bcar));
    assertThat(exec(commandObjects.hexpire(bfoo, seconds2, ExpiryOption.NX, bbar1, bbar3)), equalTo(asList(0L, 1L)));

    assertThat(exec(commandObjects.httl(bfoo, bbar1, bbar2, bbar3)),
        contains(greaterThanOrEqualTo(seconds1 - 1), equalTo(-1L),
            both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1))));
  }

  @Test
  public void hpexpireAndHpttl() {
    long millis1 = 20_000;
    long millis2 = 10_000;

    exec(commandObjects.hset("foo", "bar", "car"));
    assertThat(exec(commandObjects.hpexpire("foo", millis1, "bar", "bared")), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset("foo", "bared", "cared"));
    assertThat(exec(commandObjects.hpexpire("foo", millis2, ExpiryOption.XX, "bar", "bared")), equalTo(asList(1L, 0L)));

    assertThat(exec(commandObjects.hpttl("foo", "bar", "bare", "bared")),
        contains(both(lessThanOrEqualTo(millis2)).and(greaterThan(millis2 - 10)), equalTo(-2L), equalTo(-1L)));
  }

  @Test
  public void hpexpireAndHpttlBinary() {
    long millis1 = 20_000;
    long millis2 = 10_000;

    exec(commandObjects.hset(bfoo, bbar1, bcar));
    assertThat(exec(commandObjects.hpexpire(bfoo, millis1, bbar1, bbar3)), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset(bfoo, bbar3, bcar));
    assertThat(exec(commandObjects.hpexpire(bfoo, millis2, ExpiryOption.XX, bbar1, bbar3)), equalTo(asList(1L, 0L)));

    assertThat(exec(commandObjects.hpttl(bfoo, bbar1, bbar2, bbar3)),
        contains(both(lessThanOrEqualTo(millis2)).and(greaterThan(millis2 - 10)), equalTo(-2L), equalTo(-1L)));
  }

  @Test
  public void hexpireAtAndExpireTime() {
    long currSeconds = System.currentTimeMillis() / 1000;
    long seconds1 = currSeconds + 20;
    long seconds2 = currSeconds + 10;

    exec(commandObjects.hset("foo", "bar", "car"));
    exec(commandObjects.hset("foo", "bare", "care"));
    assertThat(exec(commandObjects.hexpireAt("foo", seconds1, "bar", "bared")), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset("foo", "bared", "cared"));
    assertThat(exec(commandObjects.hexpireAt("foo", seconds2, ExpiryOption.LT, "bar", "bared")), equalTo(asList(1L, 1L)));

    assertThat(exec(commandObjects.hexpireTime("foo", "bar", "bare", "bared")),
        contains(both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1)), equalTo(-1L),
            both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1))));
  }

  @Test
  public void hexpireAtAndExpireTimeBinary() {
    long currSeconds = System.currentTimeMillis() / 1000;
    long seconds1 = currSeconds + 20;
    long seconds2 = currSeconds + 10;

    exec(commandObjects.hset(bfoo, bbar1, bcar));
    exec(commandObjects.hset(bfoo, bbar2, bcar));
    assertThat(exec(commandObjects.hexpireAt(bfoo, seconds1, bbar1, bbar3)), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset(bfoo, bbar3, bcar));
    assertThat(exec(commandObjects.hexpireAt(bfoo, seconds2, ExpiryOption.LT, bbar1, bbar3)), equalTo(asList(1L, 1L)));

    assertThat(exec(commandObjects.hexpireTime(bfoo, bbar1, bbar2, bbar3)),
        contains(both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1)), equalTo(-1L),
            both(lessThanOrEqualTo(seconds2)).and(greaterThanOrEqualTo(seconds2 - 1))));
  }

  @Test
  public void hpexpireAtAndPexpireTime() {
    long currMillis = System.currentTimeMillis();
    long unixMillis = currMillis + 20_000;

    exec(commandObjects.hset("foo", "bar", "car"));
    assertThat(exec(commandObjects.hpexpireAt("foo", unixMillis - 100, "bar", "bared")), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset("foo", "bared", "cared"));
    assertThat(exec(commandObjects.hpexpireAt("foo", unixMillis, ExpiryOption.GT, "bar", "bared")), equalTo(asList(1L, 0L)));

    assertThat(exec(commandObjects.hpexpireTime("foo", "bar", "bare", "bared")),
        contains(equalTo(unixMillis), equalTo(-2L), equalTo(-1L)));
  }

  @Test
  public void hpexpireAtAndPexpireTimeBinary() {
    long currMillis = System.currentTimeMillis();
    long unixMillis = currMillis + 20_000;

    exec(commandObjects.hset(bfoo, bbar1, bcar));
    assertThat(exec(commandObjects.hpexpireAt(bfoo, unixMillis - 100, bbar1, bbar3)), equalTo(asList(1L, -2L)));

    exec(commandObjects.hset(bfoo, bbar3, bcar));
    assertThat(exec(commandObjects.hpexpireAt(bfoo, unixMillis, ExpiryOption.GT, bbar1, bbar3)), equalTo(asList(1L, 0L)));

    assertThat(exec(commandObjects.hpexpireTime(bfoo, bbar1, bbar2, bbar3)),
        contains(equalTo(unixMillis), equalTo(-2L), equalTo(-1L)));
  }

  @Test
  public void hpersist() {
    long seconds = 20;

    exec(commandObjects.hset("foo", "bar", "car"));
    exec(commandObjects.hset("foo", "bare", "care"));
    assertThat(exec(commandObjects.hexpire("foo", seconds, "bar", "bared")), equalTo(asList(1L, -2L)));

    assertThat(exec(commandObjects.hpersist("foo", "bar", "bare", "bared")), equalTo(asList(1L, -1L, -2L)));

    assertThat(exec(commandObjects.httl("foo", "bar", "bare", "bared")),
        contains(equalTo(-1L), equalTo(-1L), equalTo(-2L)));
  }

  @Test
  public void hpersistBinary() {
    long seconds = 20;

    exec(commandObjects.hset(bfoo, bbar1, bcar));
    exec(commandObjects.hset(bfoo, bbar2, bcar));
    assertThat(exec(commandObjects.hexpire(bfoo, seconds, bbar1, bbar3)), equalTo(asList(1L, -2L)));

    assertThat(exec(commandObjects.hpersist(bfoo, bbar1, bbar2, bbar3)), equalTo(asList(1L, -1L, -2L)));

    assertThat(exec(commandObjects.httl(bfoo, bbar1, bbar2, bbar3)),
        contains(equalTo(-1L), equalTo(-1L), equalTo(-2L)));
  }
}
