package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Tests related to <a href="https://redis.io/commands/?group=generic">Generic</a> commands.
 */
public class CommandObjectsGenericCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsGenericCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testExists() {
    String key1 = "existsKey1";
    String key2 = "existsKey2";
    String value = "value";

    exec(commandObjects.set(key1, value));
    exec(commandObjects.set(key2, value));

    Boolean existsSingle = exec(commandObjects.exists(key1));
    assertThat(existsSingle, equalTo(true));

    Long existsMultiple = exec(commandObjects.exists(key1, key2, "nonExistingKey"));
    assertThat(existsMultiple, equalTo(2L));

    Boolean existsSingleByte = exec(commandObjects.exists(key1.getBytes()));
    assertThat(existsSingleByte, equalTo(true));

    Long existsMultipleByte = exec(commandObjects.exists(key1.getBytes(), key2.getBytes(), "nonExistingKey".getBytes()));
    assertThat(existsMultipleByte, equalTo(2L));

    Boolean existsNonExisting = exec(commandObjects.exists("nonExistingKey"));
    assertThat(existsNonExisting, equalTo(false));

    Boolean existsNonExistingBytes = exec(commandObjects.exists("nonExistingKey".getBytes()));
    assertThat(existsNonExistingBytes, equalTo(false));
  }

  @Test
  public void testPersist() {
    String key1 = "persistKey1";
    byte[] key2 = "persistKey2".getBytes();
    String value = "value";
    int expireTime = 10; // seconds

    exec(commandObjects.setex(key1, expireTime, value));
    exec(commandObjects.setex(key2, expireTime, value.getBytes()));

    Long ttlBeforePersist1 = exec(commandObjects.ttl(key1));
    assertThat(ttlBeforePersist1, greaterThan(0L));

    Long ttlBeforePersist2 = exec(commandObjects.ttl(key2));
    assertThat(ttlBeforePersist2, greaterThan(0L));

    Long persist1 = exec(commandObjects.persist(key1));
    assertThat(persist1, equalTo(1L));

    Long persist2 = exec(commandObjects.persist(key2));
    assertThat(persist2, equalTo(1L));

    Long ttlAfterPersist1 = exec(commandObjects.ttl(key1));
    assertThat(ttlAfterPersist1, equalTo(-1L));

    Long ttlAfterPersist2 = exec(commandObjects.ttl(key2));
    assertThat(ttlAfterPersist2, equalTo(-1L));
  }

  @Test
  public void testType() {
    String stringKey = "stringKey";
    String listKey = "listKey";
    byte[] hashKey = "hashKey".getBytes();

    exec(commandObjects.set(stringKey, "value"));
    exec(commandObjects.rpush(listKey, "value"));
    exec(commandObjects.hset(hashKey, "field".getBytes(), "hvalue".getBytes()));

    String stringKeyType = exec(commandObjects.type(stringKey));
    assertThat(stringKeyType, equalTo("string"));

    String listKeyType = exec(commandObjects.type(listKey));
    assertThat(listKeyType, equalTo("list"));

    String hashKeyType = exec(commandObjects.type(hashKey));
    assertThat(hashKeyType, equalTo("hash"));

    String nonExistingKeyType = exec(commandObjects.type("nonExistingKey"));
    assertThat(nonExistingKeyType, equalTo("none"));
  }

  @Test
  public void testDumpAndRestore() {
    String key = "dumpRestoreKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    byte[] dumpedValue = exec(commandObjects.dump(key));
    assertThat(dumpedValue, notNullValue());

    exec(commandObjects.del(key));

    Boolean existsAfterDel = exec(commandObjects.exists(key));
    assertThat(existsAfterDel, equalTo(false));

    String restore = exec(commandObjects.restore(key, 0, dumpedValue));
    assertThat(restore, equalTo("OK"));

    String restoredValue = exec(commandObjects.get(key));
    assertThat(restoredValue, equalTo(value));

    Long ttlAfterRestore = exec(commandObjects.pttl(key));
    assertThat(ttlAfterRestore, equalTo(-1L));

    exec(commandObjects.del(key));

    Boolean existsAfterSecondDel = exec(commandObjects.exists(key));
    assertThat(existsAfterSecondDel, equalTo(false));

    long ttl = 5000; // milliseconds
    RestoreParams params = new RestoreParams().idleTime(500);

    String restoreWithParams = exec(commandObjects.restore(key, ttl, dumpedValue, params));
    assertThat(restoreWithParams, equalTo("OK"));

    String secondRestoredValue = exec(commandObjects.get(key));
    assertThat(secondRestoredValue, equalTo(value));

    Long ttlAfterSecondRestore = exec(commandObjects.pttl(key));
    assertThat(ttlAfterSecondRestore, greaterThan(0L));
  }

  @Test
  public void testDumpAndRestoreBinary() {
    byte[] key = "dumpRestoreKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    byte[] dumpedValue = exec(commandObjects.dump(key));
    assertThat(dumpedValue, notNullValue());

    exec(commandObjects.del(key));

    Boolean existsAfterDel = exec(commandObjects.exists(key));
    assertThat(existsAfterDel, equalTo(false));

    String restore = exec(commandObjects.restore(key, 0, dumpedValue));
    assertThat(restore, equalTo("OK"));

    byte[] restoredValue = exec(commandObjects.get(key));
    assertThat(restoredValue, equalTo(value));

    Long ttlAfterRestore = exec(commandObjects.pttl(key));
    assertThat(ttlAfterRestore, equalTo(-1L));

    exec(commandObjects.del(key));

    Boolean existsAfterSecondDel = exec(commandObjects.exists(key));
    assertThat(existsAfterSecondDel, equalTo(false));

    long ttl = 5000; // milliseconds
    RestoreParams params = new RestoreParams().idleTime(500);

    String restoreWithParams = exec(commandObjects.restore(key, ttl, dumpedValue, params));
    assertThat(restoreWithParams, equalTo("OK"));

    byte[] secondRestoredValue = exec(commandObjects.get(key));
    assertThat(secondRestoredValue, equalTo(value));

    Long ttlAfterSecondRestore = exec(commandObjects.pttl(key));
    assertThat(ttlAfterSecondRestore, greaterThan(0L));
  }

  @Test
  public void testExpireAndExpireTime() {
    String key = "expireKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    Long expireTimeBefore = exec(commandObjects.expireTime(key));
    assertThat(expireTimeBefore, equalTo(-1L));

    long seconds = 60;

    Long expire = exec(commandObjects.expire(key, seconds));
    assertThat(expire, equalTo(1L));

    Long expireTimeAfter = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfter, greaterThan(System.currentTimeMillis() / 1000));
  }

  @Test
  public void testExpireAndExpireTimeBinary() {
    byte[] key = "expireKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    Long expireTimeBefore = exec(commandObjects.expireTime(key));
    assertThat(expireTimeBefore, equalTo(-1L));

    long seconds = 60;

    Long expire = exec(commandObjects.expire(key, seconds));
    assertThat(expire, equalTo(1L));

    Long expireTimeAfter = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfter, greaterThan(System.currentTimeMillis() / 1000));
  }

  @Test
  public void testExpireWithExpiryOption() {
    String key = "expireWithOptionKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    Long expireTimeBefore = exec(commandObjects.expireTime(key));
    assertThat(expireTimeBefore, equalTo(-1L));

    long seconds = 120;
    ExpiryOption expiryOptionNX = ExpiryOption.NX;

    Long expireNx = exec(commandObjects.expire(key, seconds, expiryOptionNX));
    assertThat(expireNx, equalTo(1L));

    Long expireNxAgain = exec(commandObjects.expire(key, seconds, expiryOptionNX));
    assertThat(expireNxAgain, equalTo(0L));

    Long expireTimeAfter = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfter, greaterThan(System.currentTimeMillis() / 1000));
  }

  @Test
  public void testExpireWithExpiryOptionTimeBinary() {
    byte[] key = "expireWithOptionKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    Long expireTimeBefore = exec(commandObjects.expireTime(key));
    assertThat(expireTimeBefore, equalTo(-1L));

    long seconds = 120;
    ExpiryOption expiryOptionNX = ExpiryOption.NX;

    Long expireNx = exec(commandObjects.expire(key, seconds, expiryOptionNX));
    assertThat(expireNx, equalTo(1L));

    Long expireNxAgain = exec(commandObjects.expire(key, seconds, expiryOptionNX));
    assertThat(expireNxAgain, equalTo(0L));

    Long expireTimeAfter = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfter, greaterThan(System.currentTimeMillis() / 1000));
  }

  @Test
  public void testPexpireAndPexpireTime() {
    String key = "pexpireKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long expireTimeMillis = 15000; // 15 seconds

    Long pexpireTimeBefore = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeBefore, equalTo(-1L));

    Long pexpire = exec(commandObjects.pexpire(key, expireTimeMillis));
    assertThat(pexpire, equalTo(1L));

    Long pexpireTimeAfter = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfter, greaterThan(System.currentTimeMillis()));
  }

  @Test
  public void testPexpireAndPexpireTimeBinary() {
    byte[] key = "pexpireKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long expireTimeMillis = 15000; // 15 seconds

    Long pexpireTimeBefore = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeBefore, equalTo(-1L));

    Long pexpire = exec(commandObjects.pexpire(key, expireTimeMillis));
    assertThat(pexpire, equalTo(1L));

    Long pexpireTimeAfter = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfter, greaterThan(System.currentTimeMillis()));
  }

  @Test
  public void testPexpireWithOptionsAndPexpireTime() {
    String key = "pexpireWithOptionsKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long expireTimeMillis = 20000; // 20 seconds

    Long pexpireTimeBefore = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeBefore, equalTo(-1L));

    Long pexpire = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.NX));
    assertThat(pexpire, equalTo(1L));

    Long pexpireTimeAfterSet = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterSet, greaterThan(System.currentTimeMillis()));

    Long pexpireWithNx = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.NX));
    assertThat(pexpireWithNx, equalTo(0L));

    Long pexpireWithXx = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.XX));
    assertThat(pexpireWithXx, equalTo(1L));
  }

  @Test
  public void testPexpireWithOptionsAndPexpireTimeBinary() {
    byte[] key = "pexpireWithOptionsKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long expireTimeMillis = 20000; // 20 seconds

    Long pexpireTimeBefore = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeBefore, equalTo(-1L));

    Long pexpire = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.NX));
    assertThat(pexpire, equalTo(1L));

    Long pexpireTimeAfterSet = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterSet, greaterThan(System.currentTimeMillis()));

    Long pexpireWithNx = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.NX));
    assertThat(pexpireWithNx, equalTo(0L));

    Long pexpireWithXx = exec(commandObjects.pexpire(key, expireTimeMillis, ExpiryOption.XX));
    assertThat(pexpireWithXx, equalTo(1L));
  }

  @Test
  public void testExpireAtAndExpireTime() {
    String key = "expireAtKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long futureExpireTime = System.currentTimeMillis() / 1000 + 10; // 10 seconds from now

    // Setting expire at in the future
    Long expireAt = exec(commandObjects.expireAt(key, futureExpireTime));
    assertThat(expireAt, equalTo(1L));

    Long expireTime = exec(commandObjects.expireTime(key));
    assertThat(expireTime, equalTo(futureExpireTime));

    // Setting expire at in the past should delete the key
    long pastExpireTime = System.currentTimeMillis() / 1000 - 10;
    Long expireAtPast = exec(commandObjects.expireAt(key, pastExpireTime));
    assertThat(expireAtPast, equalTo(1L));

    Long expireTimeAfterPast = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterPast, equalTo(-2L)); // Key does not exist
  }

  @Test
  public void testExpireAtAndExpireTimeBinary() {
    byte[] key = "expireAtKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long futureExpireTime = System.currentTimeMillis() / 1000 + 10; // 10 seconds from now

    // Setting expire at in the future
    Long expireAt = exec(commandObjects.expireAt(key, futureExpireTime));
    assertThat(expireAt, equalTo(1L));

    Long expireTime = exec(commandObjects.expireTime(key));
    assertThat(expireTime, equalTo(futureExpireTime));

    // Setting expire at in the past should delete the key
    long pastExpireTime = System.currentTimeMillis() / 1000 - 10;
    Long expireAtPast = exec(commandObjects.expireAt(key, pastExpireTime));
    assertThat(expireAtPast, equalTo(1L));

    Long expireTimeAfterPast = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterPast, equalTo(-2L)); // Key does not exist
  }

  @Test
  public void testExpireAtWithOptionsAndExpireTime() {
    String key = "expireAtWithOptionsKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long futureExpireTime = System.currentTimeMillis() / 1000 + 20; // 20 seconds from now

    // Setting expire at in the future, with NX
    Long expireAtNx = exec(commandObjects.expireAt(key, futureExpireTime, ExpiryOption.NX));
    assertThat(expireAtNx, equalTo(1L));

    Long expireTimeAfterNx = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterNx, equalTo(futureExpireTime));

    // Update expire at in the future, with XX
    long laterFutureExpireTime = futureExpireTime + 10;
    Long expireAtXx = exec(commandObjects.expireAt(key, laterFutureExpireTime, ExpiryOption.XX));
    assertThat(expireAtXx, equalTo(1L));

    Long expireTimeAfterXx = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterXx, equalTo(laterFutureExpireTime));

    // Try to reset with NX, should fail
    Long expireAtNxAgain = exec(commandObjects.expireAt(key, futureExpireTime, ExpiryOption.NX));
    assertThat(expireAtNxAgain, equalTo(0L));

    Long expireTimeAfterNxAgain = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterNxAgain, equalTo(laterFutureExpireTime));
  }

  @Test
  public void testExpireAtWithOptionsAndExpireTimeBinary() {
    byte[] key = "expireAtWithOptionsKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long futureExpireTime = System.currentTimeMillis() / 1000 + 20; // 20 seconds from now

    // Setting expire at in the future, with NX
    Long expireAtNx = exec(commandObjects.expireAt(key, futureExpireTime, ExpiryOption.NX));
    assertThat(expireAtNx, equalTo(1L));

    Long expireTimeAfterNx = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterNx, equalTo(futureExpireTime));

    // Update expire at in the future, with XX
    long laterFutureExpireTime = futureExpireTime + 10;
    Long expireAtXx = exec(commandObjects.expireAt(key, laterFutureExpireTime, ExpiryOption.XX));
    assertThat(expireAtXx, equalTo(1L));

    Long expireTime = exec(commandObjects.expireTime(key));
    assertThat(expireTime, equalTo(laterFutureExpireTime));

    // Try to reset with NX, should fail
    Long expireAtNxAgain = exec(commandObjects.expireAt(key, futureExpireTime, ExpiryOption.NX));
    assertThat(expireAtNxAgain, equalTo(0L));

    Long expireTimeAfterNxAgain = exec(commandObjects.expireTime(key));
    assertThat(expireTimeAfterNxAgain, equalTo(laterFutureExpireTime));
  }

  @Test
  public void testPexpireAtAndPexpireTime() {
    String key = "pexpireAtKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long futureTimestampMillis = System.currentTimeMillis() + 20000; // 20 seconds from now

    Long pexpireAt = exec(commandObjects.pexpireAt(key, futureTimestampMillis));
    assertThat(pexpireAt, equalTo(1L));

    Long pexpireTime = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTime, equalTo(futureTimestampMillis));

    // Setting pexpire at a past timestamp should delete the key
    long pastTimestampMillis = System.currentTimeMillis() - 20000;
    Long pexpireAtPast = exec(commandObjects.pexpireAt(key, pastTimestampMillis));
    assertThat(pexpireAtPast, equalTo(1L));

    Long pexpireTimeAfterPast = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterPast, equalTo(-2L)); // Key does not exist
  }

  @Test
  public void testPexpireAtAndPexpireTimeBinary() {
    byte[] key = "pexpireAtKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long futureTimestampMillis = System.currentTimeMillis() + 20000; // 20 seconds from now

    Long pexpireAt = exec(commandObjects.pexpireAt(key, futureTimestampMillis));
    assertThat(pexpireAt, equalTo(1L));

    Long pexpireTime = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTime, equalTo(futureTimestampMillis));

    // Setting pexpire at a past timestamp should delete the key
    long pastTimestampMillis = System.currentTimeMillis() - 20000;
    Long pexpireAtPast = exec(commandObjects.pexpireAt(key, pastTimestampMillis));
    assertThat(pexpireAtPast, equalTo(1L));

    Long pexpireTimeAfterPast = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterPast, equalTo(-2L)); // Key does not exist
  }

  @Test
  public void testPexpireAtWithOptionsAndPexpireTime() {
    String key = "pexpireAtWithOptionsKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    long futureTimestampMillis = System.currentTimeMillis() + 30000; // 30 seconds from now

    // Setting with NX
    Long pexpireAtNx = exec(commandObjects.pexpireAt(key, futureTimestampMillis, ExpiryOption.NX));
    assertThat(pexpireAtNx, equalTo(1L));

    Long pexpireTimeAfterNx = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterNx, equalTo(futureTimestampMillis));

    // Updating with XX
    long laterFutureTimestampMillis = futureTimestampMillis + 10000; // Further 10 seconds in the future
    Long pexpireAtXx = exec(commandObjects.pexpireAt(key, laterFutureTimestampMillis, ExpiryOption.XX));
    assertThat(pexpireAtXx, equalTo(1L));

    Long pexpireTimeAfterXx = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterXx, equalTo(laterFutureTimestampMillis));

    // Updating with NX fails
    Long pexpireAtNxAgain = exec(commandObjects.pexpireAt(key, futureTimestampMillis, ExpiryOption.NX));
    assertThat(pexpireAtNxAgain, equalTo(0L));

    Long pexpireTimeAfterNxAgain = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterNxAgain, equalTo(laterFutureTimestampMillis));
  }

  @Test
  public void testPexpireAtWithOptionsAndPexpireTimeBinary() {
    byte[] key = "pexpireAtWithOptionsKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    long futureTimestampMillis = System.currentTimeMillis() + 30000; // 30 seconds from now

    // Setting with NX
    Long pexpireAtNx = exec(commandObjects.pexpireAt(key, futureTimestampMillis, ExpiryOption.NX));
    assertThat(pexpireAtNx, equalTo(1L));

    Long pexpireTimeAfterNx = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterNx, equalTo(futureTimestampMillis));

    // Updating with XX
    long laterFutureTimestampMillis = futureTimestampMillis + 10000; // Further 10 seconds in the future
    Long pexpireAtXx = exec(commandObjects.pexpireAt(key, laterFutureTimestampMillis, ExpiryOption.XX));
    assertThat(pexpireAtXx, equalTo(1L));

    Long pexpireTimeAfterXx = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterXx, equalTo(laterFutureTimestampMillis));

    // Updating with NX fails
    Long pexpireAtNxAgain = exec(commandObjects.pexpireAt(key, futureTimestampMillis, ExpiryOption.NX));
    assertThat(pexpireAtNxAgain, equalTo(0L));

    Long pexpireTimeAfterNxAgain = exec(commandObjects.pexpireTime(key));
    assertThat(pexpireTimeAfterNxAgain, equalTo(laterFutureTimestampMillis));
  }

  @Test
  public void testTtl() {
    String key = "ttlKey";
    String value = "value";

    long seconds = 10;

    exec(commandObjects.set(key, value));
    exec(commandObjects.expire(key, seconds));

    Long ttl = exec(commandObjects.ttl(key));
    assertThat(ttl, greaterThan(0L));
  }

  @Test
  public void testTtlBinary() {
    byte[] key = "ttlKey".getBytes();
    byte[] value = "value".getBytes();

    long seconds = 10;

    exec(commandObjects.set(key, value));
    exec(commandObjects.expire(key, seconds));

    Long ttl = exec(commandObjects.ttl(key));
    assertThat(ttl, greaterThan(1L));
  }

  @Test
  public void testPttl() {
    String key = "pttlKey";
    String value = "value";

    long milliseconds = 10000; // 10 seconds

    exec(commandObjects.set(key, value));
    exec(commandObjects.pexpire(key, milliseconds));

    Long pttl = exec(commandObjects.pttl(key));
    assertThat(pttl, greaterThan(0L));
  }

  @Test
  public void testPttlBinary() {
    byte[] key = "pttlKey".getBytes();
    byte[] value = "value".getBytes();

    long milliseconds = 10000; // 10 seconds

    exec(commandObjects.set(key, value));
    exec(commandObjects.pexpire(key, milliseconds));

    Long pttl = exec(commandObjects.pttl(key));
    assertThat(pttl, greaterThan(1L));
  }

  @Test
  public void testTouch() {
    String key = "touchKey";

    exec(commandObjects.set(key, "value"));

    Long touchExisting = exec(commandObjects.touch(key));
    assertThat(touchExisting, equalTo(1L));

    Long touchNonExistent = exec(commandObjects.touch("nonExistentKey"));
    assertThat(touchNonExistent, equalTo(0L));
  }

  @Test
  public void testTouchBinary() {
    byte[] key = "touchKey".getBytes();

    exec(commandObjects.set(key, "value".getBytes()));

    Long touchExisting = exec(commandObjects.touch(key));
    assertThat(touchExisting, equalTo(1L));

    Long touchNonExistent = exec(commandObjects.touch("nonExistentKey".getBytes()));
    assertThat(touchNonExistent, equalTo(0L));
  }

  @Test
  public void testTouchMultiple() {
    String key1 = "touchMultiKey1";
    String key2 = "touchMultiKey2";
    String key3 = "nonExistentKey";

    exec(commandObjects.set(key1, "value1"));
    exec(commandObjects.set(key2, "value2"));

    Long touch = exec(commandObjects.touch(key1, key2, key3));
    assertThat(touch, equalTo(2L));
  }

  @Test
  public void testTouchMultipleBinary() {
    byte[] key1 = "touchMultiKey1".getBytes();
    byte[] key2 = "touchMultiKey2".getBytes();
    byte[] key3 = "nonExistentKey".getBytes();

    exec(commandObjects.set(key1, "value1".getBytes()));
    exec(commandObjects.set(key2, "value2".getBytes()));

    Long touch = exec(commandObjects.touch(key1, key2, key3));
    assertThat(touch, equalTo(2L));
  }

  @Test
  public void testSort() {
    String listKey = "sortList";

    exec(commandObjects.lpush(listKey, "3", "1", "2"));

    List<String> sorted = exec(commandObjects.sort(listKey));
    assertThat(sorted, contains("1", "2", "3"));
  }

  @Test
  public void testSortBinary() {
    byte[] listKey = "sortList".getBytes();

    exec(commandObjects.lpush(listKey, "3".getBytes(), "1".getBytes(), "2".getBytes()));

    List<byte[]> sorted = exec(commandObjects.sort(listKey));
    assertThat(sorted, contains("1".getBytes(), "2".getBytes(), "3".getBytes()));
  }

  @Test
  public void testSortWithSortingParams() {
    String listKey = "sortListParams";

    exec(commandObjects.lpush(listKey, "item3", "item1", "item2"));

    SortingParams sortingParams = new SortingParams().alpha().limit(0, 2);

    List<String> sorted = exec(commandObjects.sort(listKey, sortingParams));
    assertThat(sorted, contains("item1", "item2"));
  }

  @Test
  public void testSortBinaryWithSortingParams() {
    byte[] listKey = "sortListParams".getBytes();

    exec(commandObjects.lpush(listKey, "item3".getBytes(), "item1".getBytes(), "item2".getBytes()));

    SortingParams sortingParams = new SortingParams().alpha().limit(0, 2);

    List<byte[]> sorted = exec(commandObjects.sort(listKey, sortingParams));
    assertThat(sorted, contains("item1".getBytes(), "item2".getBytes()));
  }

  @Test
  public void testSortAndStore() {
    String listKey = "sortStoreList";
    String destinationKey = "sortedList";

    exec(commandObjects.lpush(listKey, "9", "3", "6"));

    Long sort = exec(commandObjects.sort(listKey, destinationKey));
    assertThat(sort, equalTo(3L));

    List<String> sorted = exec(commandObjects.lrange(destinationKey, 0, -1));
    assertThat(sorted, contains("3", "6", "9"));
  }

  @Test
  public void testSortAndStoreBinary() {
    byte[] listKey = "sortStoreList".getBytes();
    byte[] destinationKey = "sortedList".getBytes();

    exec(commandObjects.lpush(listKey, "9".getBytes(), "3".getBytes(), "6".getBytes()));

    Long sort = exec(commandObjects.sort(listKey, destinationKey));
    assertThat(sort, equalTo(3L));

    List<byte[]> sorted = exec(commandObjects.lrange(destinationKey, 0, -1));
    assertThat(sorted, contains("3".getBytes(), "6".getBytes(), "9".getBytes()));
  }

  @Test
  public void testSortWithParamsAndStore() {
    String listKey = "sortParamsStoreList";
    String destinationKey = "sortedParamsList";

    exec(commandObjects.lpush(listKey, "item3", "item1", "item2"));

    SortingParams sortingParams = new SortingParams().alpha().limit(0, 2);

    Long sort = exec(commandObjects.sort(listKey, sortingParams, destinationKey));
    assertThat(sort, equalTo(2L));

    List<String> sorted = exec(commandObjects.lrange(destinationKey, 0, -1));
    assertThat(sorted, contains("item1", "item2"));
  }

  @Test
  public void testSortWithParamsAndStoreBinary() {
    byte[] listKey = "sortParamsStoreList".getBytes();
    byte[] destinationKey = "sortedParamsList".getBytes();

    exec(commandObjects.lpush(listKey, "item3".getBytes(), "item1".getBytes(), "item2".getBytes()));

    SortingParams sortingParams = new SortingParams().alpha().limit(0, 2);

    Long sort = exec(commandObjects.sort(listKey, sortingParams, destinationKey));
    assertThat(sort, equalTo(2L));

    List<byte[]> sorted = exec(commandObjects.lrange(destinationKey, 0, -1));
    assertThat(sorted, contains("item1".getBytes(), "item2".getBytes()));
  }

  @Test
  public void testSortReadonly() {
    String listKey = "readonlySortList";

    exec(commandObjects.lpush(listKey, "3", "1", "2"));

    SortingParams sortingParams = new SortingParams().desc();

    List<String> sorted = exec(commandObjects.sortReadonly(listKey, sortingParams));
    assertThat(sorted, contains("3", "2", "1"));
  }

  @Test
  public void testSortReadonlyBinary() {
    byte[] listKey = "readonlySortList".getBytes();

    exec(commandObjects.lpush(listKey, "3".getBytes(), "1".getBytes(), "2".getBytes()));

    SortingParams sortingParams = new SortingParams().desc();

    List<byte[]> sorted = exec(commandObjects.sortReadonly(listKey, sortingParams));
    assertThat(sorted, contains("3".getBytes(), "2".getBytes(), "1".getBytes()));
  }

  @Test
  public void testDel() {
    String key = "delKey";
    String value = "value";

    exec(commandObjects.set(key, value));

    String getBeforeDel = exec(commandObjects.get(key));
    assertThat(getBeforeDel, equalTo(value));

    Long del = exec(commandObjects.del(key));
    assertThat(del, equalTo(1L));

    String getAfterDel = exec(commandObjects.get(key));
    assertThat(getAfterDel, nullValue());
  }

  @Test
  public void testDelBinary() {
    byte[] key = "delKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    byte[] getBeforeDel = exec(commandObjects.get(key));
    assertThat(getBeforeDel, equalTo(value));

    Long del = exec(commandObjects.del(key));
    assertThat(del, equalTo(1L));

    byte[] getAfterDel = exec(commandObjects.get(key));
    assertThat(getAfterDel, nullValue());
  }

  @Test
  public void testDelMultiple() {
    String key1 = "key1";
    String key2 = "key2";

    exec(commandObjects.set(key1, "value"));
    exec(commandObjects.set(key2, "value"));

    Long del = exec(commandObjects.del(key1, key2, "nonExistingKey"));
    assertThat(del, equalTo(2L));

    Long exists = exec(commandObjects.exists(key1, key2));
    assertThat(exists, equalTo(0L));
  }

  @Test
  public void testDelMultipleBinary() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    exec(commandObjects.set(key1, "value".getBytes()));
    exec(commandObjects.set(key2, "value".getBytes()));

    Long del = exec(commandObjects.del(key1, key2, "nonExistingKey".getBytes()));
    assertThat(del, equalTo(2L));

    Long exists = exec(commandObjects.exists(key1, key2));
    assertThat(exists, equalTo(0L));
  }

  @Test
  public void testUnlink() {
    String key = "unlinkKey";

    exec(commandObjects.set(key, "value"));

    Long unlink = exec(commandObjects.unlink(key));
    assertThat(unlink, equalTo(1L));

    Boolean exists = exec(commandObjects.exists(key));
    assertThat(exists, equalTo(false));
  }

  @Test
  public void testUnlinkBinary() {
    byte[] key = "unlinkKey".getBytes();

    exec(commandObjects.set(key, "value".getBytes()));

    Long unlink = exec(commandObjects.unlink(key));
    assertThat(unlink, equalTo(1L));

    Boolean exists = exec(commandObjects.exists(key));
    assertThat(exists, equalTo(false));
  }

  @Test
  public void testUnlinkMultiple() {
    String key1 = "key1ToUnlink";
    String key2 = "key2ToUnlink";

    exec(commandObjects.set(key1, "value"));
    exec(commandObjects.set(key2, "value"));

    Long unlink = exec(commandObjects.unlink(key1, key2, "nonExistingKey"));
    assertThat(unlink, equalTo(2L));

    Long exists = exec(commandObjects.exists(key1, key2));
    assertThat(exists, equalTo(0L));
  }

  @Test
  public void testUnlinkMultipleBinary() {
    byte[] key1 = "key1ToUnlink".getBytes();
    byte[] key2 = "key2ToUnlink".getBytes();

    exec(commandObjects.set(key1, "value".getBytes()));
    exec(commandObjects.set(key2, "value".getBytes()));

    Long unlink = exec(commandObjects.unlink(key1, key2, "nonExistingKey".getBytes()));
    assertThat(unlink, equalTo(2L));

    Long exists = exec(commandObjects.exists(key1, key2));
    assertThat(exists, equalTo(0L));
  }

  @Test
  public void testCopyWithStringKeys() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    String value = "value";
    String otherValue = "otherValue";

    exec(commandObjects.set(srcKey, value));

    String initialValue = exec(commandObjects.get(srcKey));
    assertThat(initialValue, equalTo(value));

    String dstBeforeCopy = exec(commandObjects.get(dstKey));
    assertThat(dstBeforeCopy, nullValue());

    Boolean copy = exec(commandObjects.copy(srcKey, dstKey, false));
    assertThat(copy, equalTo(true));

    String dstAfterCopy = exec(commandObjects.get(dstKey));
    assertThat(dstAfterCopy, equalTo(value));

    exec(commandObjects.set(srcKey, otherValue));

    Boolean copyFail = exec(commandObjects.copy(srcKey, dstKey, false));
    assertThat(copyFail, equalTo(false));

    String dstAfterFailedCopy = exec(commandObjects.get(dstKey));
    assertThat(dstAfterFailedCopy, equalTo(value));

    Boolean copyReplace = exec(commandObjects.copy(srcKey, dstKey, true));
    assertThat(copyReplace, equalTo(true));

    String dstAfterReplace = exec(commandObjects.get(dstKey));
    assertThat(dstAfterReplace, equalTo(otherValue));
  }

  @Test
  public void testCopyWithBinaryKeys() {
    byte[] srcKey = "sourceKey".getBytes();
    byte[] dstKey = "destinationKey".getBytes();
    byte[] value = "value".getBytes();
    byte[] otherValue = "otherValue".getBytes();

    exec(commandObjects.set(srcKey, value));

    byte[] initialValue = exec(commandObjects.get(srcKey));
    assertThat(initialValue, equalTo(value));

    byte[] dstBeforeCopy = exec(commandObjects.get(dstKey));
    assertThat(dstBeforeCopy, nullValue());

    Boolean copy = exec(commandObjects.copy(srcKey, dstKey, false));
    assertThat(copy, equalTo(true));

    byte[] dstAfterCopy = exec(commandObjects.get(dstKey));
    assertThat(dstAfterCopy, equalTo(value));

    exec(commandObjects.set(srcKey, otherValue));

    Boolean copyFail = exec(commandObjects.copy(srcKey, dstKey, false));
    assertThat(copyFail, equalTo(false));

    byte[] dstAfterFailedCopy = exec(commandObjects.get(dstKey));
    assertThat(dstAfterFailedCopy, equalTo(value));

    Boolean copyReplace = exec(commandObjects.copy(srcKey, dstKey, true));
    assertThat(copyReplace, equalTo(true));

    byte[] dstAfterReplace = exec(commandObjects.get(dstKey));
    assertThat(dstAfterReplace, equalTo(otherValue));
  }

  @Test
  public void testCopyToDb() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    int dstDB = 1;

    exec(commandObjects.set(srcKey, "initialValue"));

    Boolean existsAfterSet = exec(commandObjects.exists(srcKey));
    assertThat(existsAfterSet, equalTo(true));

    Boolean copy = exec(commandObjects.copy(srcKey, dstKey, dstDB, true));
    assertThat(copy, equalTo(true));

    assertKeyExists(dstDB, dstKey, "initialValue");

    // Update source
    exec(commandObjects.set(srcKey, "newValue"));

    // Copy again without replace, it fails since dstKey already exists
    Boolean secondCopy = exec(commandObjects.copy(srcKey, dstKey, dstDB, false));
    assertThat(secondCopy, equalTo(false));

    assertKeyExists(dstDB, dstKey, "initialValue");
  }

  @Test
  public void testCopyToDbBinary() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    int dstDB = 1;

    exec(commandObjects.set(srcKey, "initialValue"));

    Boolean existsAfterSet = exec(commandObjects.exists(srcKey));
    assertThat(existsAfterSet, equalTo(true));

    Boolean copy = exec(commandObjects.copy(
        srcKey.getBytes(), dstKey.getBytes(), dstDB, true));
    assertThat(copy, equalTo(true));

    assertKeyExists(dstDB, dstKey, "initialValue");

    // Update source
    exec(commandObjects.set(srcKey, "newValue"));

    // Copy again without replace, it will fail
    Boolean secondCopy = exec(commandObjects.copy(srcKey.getBytes(), dstKey.getBytes(), dstDB, false));
    assertThat(secondCopy, equalTo(false));

    assertKeyExists(dstDB, dstKey, "initialValue");
  }

  private void assertKeyExists(int dstDb, String key, Object expectedValue) {
    // Cheat and use Jedis, it gives us access to any db.
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort())) {
      jedis.auth(endpoint.getPassword());
      jedis.select(dstDb);
      assertThat(jedis.get(key), equalTo(expectedValue));
    }
  }

  @Test
  public void testRenameWithStringKeys() {
    String oldKey = "oldKeyName";
    String newKey = "newKeyName";
    String value = "value";

    exec(commandObjects.set(oldKey, value));

    String oldValue = exec(commandObjects.get(oldKey));
    assertThat(oldValue, equalTo(value));

    String newKeyBeforeRename = exec(commandObjects.get(newKey));
    assertThat(newKeyBeforeRename, nullValue());

    String rename = exec(commandObjects.rename(oldKey, newKey));
    assertThat(rename, equalTo("OK"));

    String oldKeyAfterRename = exec(commandObjects.get(oldKey));
    assertThat(oldKeyAfterRename, nullValue());

    String newValue = exec(commandObjects.get(newKey));
    assertThat(newValue, equalTo(value));
  }

  @Test
  public void testRenameWithBinaryKeys() {
    byte[] oldKey = "oldKeyName".getBytes();
    byte[] newKey = "newKeyName".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(oldKey, value));

    byte[] oldValue = exec(commandObjects.get(oldKey));
    assertThat(oldValue, equalTo(value));

    byte[] newKeyBeforeRename = exec(commandObjects.get(newKey));
    assertThat(newKeyBeforeRename, nullValue());

    String rename = exec(commandObjects.rename(oldKey, newKey));
    assertThat(rename, equalTo("OK"));

    byte[] oldKeyAfterRename = exec(commandObjects.get(oldKey));
    assertThat(oldKeyAfterRename, nullValue());

    byte[] newValue = exec(commandObjects.get(newKey));
    assertThat(newValue, equalTo(value));
  }

  @Test
  public void testRenamenx() {
    String oldKey = "oldKeyToRenameNX";
    String newKey = "newKeyForRenameNX";
    String anotherKey = "anotherKey";
    String value = "value";

    exec(commandObjects.set(oldKey, value));
    exec(commandObjects.set(anotherKey, value));

    String newKeyBefore = exec(commandObjects.get(newKey));
    assertThat(newKeyBefore, nullValue());

    Long renamenx = exec(commandObjects.renamenx(oldKey, newKey));
    assertThat(renamenx, equalTo(1L));

    String newValue = exec(commandObjects.get(newKey));
    assertThat(newValue, equalTo(value));

    Long renamenxFail = exec(commandObjects.renamenx(anotherKey, newKey));
    assertThat(renamenxFail, equalTo(0L));

    String anotherKeyStillExists = exec(commandObjects.get(anotherKey));
    assertThat(anotherKeyStillExists, equalTo(value));
  }

  @Test
  public void testRenamenxBinary() {
    byte[] oldKey = "oldKeyToRenameNX".getBytes();
    byte[] newKey = "newKeyForRenameNX".getBytes();
    byte[] anotherKey = "anotherKey".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(oldKey, value));
    exec(commandObjects.set(anotherKey, value));

    byte[] newKeyBefore = exec(commandObjects.get(newKey));
    assertThat(newKeyBefore, nullValue());

    Long renamenx = exec(commandObjects.renamenx(oldKey, newKey));
    assertThat(renamenx, equalTo(1L));

    byte[] newValue = exec(commandObjects.get(newKey));
    assertThat(newValue, equalTo(value));

    Long renamenxFail = exec(commandObjects.renamenx(anotherKey, newKey));
    assertThat(renamenxFail, equalTo(0L));

    byte[] anotherKeyStillExists = exec(commandObjects.get(anotherKey));
    assertThat(anotherKeyStillExists, equalTo(value));
  }

  @Test
  public void testDbSize() {
    Long initialSize = exec(commandObjects.dbSize());
    assertThat(initialSize, greaterThanOrEqualTo(0L));

    String key = "testKey";

    exec(commandObjects.set(key, "testValue"));

    Long newSize = exec(commandObjects.dbSize());
    assertThat(newSize, equalTo(initialSize + 1));

    exec(commandObjects.del(key));

    Long finalSize = exec(commandObjects.dbSize());
    assertThat(finalSize, equalTo(initialSize));
  }

  @Test
  public void testKeysWithStringPattern() {
    String pattern = "testKey:*";
    String matchingKey1 = "testKey:1";
    String matchingKey2 = "testKey:2";
    String value = "value";

    exec(commandObjects.set(matchingKey1, value));
    exec(commandObjects.set(matchingKey2, value));
    exec(commandObjects.set("otherKey", value));

    Set<String> keys = exec(commandObjects.keys(pattern));
    assertThat(keys, containsInAnyOrder(matchingKey1, matchingKey2));

    exec(commandObjects.del(matchingKey1, matchingKey2));

    Set<String> keysAfterDeletion = exec(commandObjects.keys(pattern));
    assertThat(keysAfterDeletion, empty());
  }

  @Test
  public void testKeysWithBinaryPattern() {
    byte[] pattern = "testKey:*".getBytes();
    byte[] matchingKey1 = "testKey:1".getBytes();
    byte[] matchingKey2 = "testKey:2".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(matchingKey1, value));
    exec(commandObjects.set(matchingKey2, value));
    exec(commandObjects.set("otherKey".getBytes(), value));

    Set<byte[]> keys = exec(commandObjects.keys(pattern));
    assertThat(keys, containsInAnyOrder(matchingKey1, matchingKey2));

    exec(commandObjects.del(matchingKey1, matchingKey2));

    Set<byte[]> keysAfterDeletion = exec(commandObjects.keys(pattern));
    assertThat(keysAfterDeletion, empty());
  }

  @Test
  public void testScan() {
    String key1 = "scanKey1";
    String key2 = "scanKey2";

    exec(commandObjects.set(key1, "value"));
    exec(commandObjects.set(key2, "value"));

    Set<String> collectedKeys = new HashSet<>();

    ScanResult<String> scanResult;
    String nextCursor = "0";

    do {
      scanResult = exec(commandObjects.scan(nextCursor));
      nextCursor = scanResult.getCursor();
      collectedKeys.addAll(scanResult.getResult());
    } while (!"0".equals(nextCursor));

    assertThat(collectedKeys, hasItems(key1, key2));
  }

  @Test
  public void testScanBinary() {
    byte[] key1 = "scanKey1".getBytes();
    byte[] key2 = "scanKey2".getBytes();

    exec(commandObjects.set(key1, "value".getBytes()));
    exec(commandObjects.set(key2, "value".getBytes()));

    Set<byte[]> collectedKeys = new HashSet<>();

    ScanResult<byte[]> scanResult;
    byte[] cursor = "0".getBytes();

    do {
      scanResult = exec(commandObjects.scan(cursor));
      cursor = scanResult.getCursorAsBytes();
      collectedKeys.addAll(scanResult.getResult());
    } while (!Arrays.equals("0".getBytes(), cursor));

    assertThat(collectedKeys, hasItems(key1, key2));
  }

  @Test
  public void testScanWithParams() {
    String matchingKey1 = "user:123";
    String matchingKey2 = "user:456";
    String nonMatchingKey = "config:123";

    exec(commandObjects.set(matchingKey1, "testValue"));
    exec(commandObjects.set(matchingKey2, "testValue"));
    exec(commandObjects.set(nonMatchingKey, "testValue"));

    ScanParams params = new ScanParams().match("user:*").count(2);

    Set<String> collectedKeys = new HashSet<>();

    ScanResult<String> scanResult;
    String cursor = "0";

    do {
      scanResult = exec(commandObjects.scan(cursor, params));
      collectedKeys.addAll(scanResult.getResult());
      cursor = scanResult.getCursor();
    } while (!"0".equals(scanResult.getCursor()));

    assertThat(collectedKeys, hasItems(matchingKey1, matchingKey2));
    assertThat(collectedKeys, not(hasItem(nonMatchingKey)));
  }

  @Test
  public void testScanWithParamsBinary() {
    byte[] matchingKey1 = "user:123".getBytes();
    byte[] matchingKey2 = "user:456".getBytes();
    byte[] nonMatchingKey = "config:123".getBytes();

    exec(commandObjects.set(matchingKey1, "testValue".getBytes()));
    exec(commandObjects.set(matchingKey2, "testValue".getBytes()));
    exec(commandObjects.set(nonMatchingKey, "testValue".getBytes()));

    ScanParams params = new ScanParams().match("user:*").count(2);

    Set<byte[]> collectedKeys = new HashSet<>();

    ScanResult<byte[]> scanResult;
    byte[] cursor = "0".getBytes();

    do {
      scanResult = exec(commandObjects.scan(cursor, params));
      collectedKeys.addAll(scanResult.getResult());
      cursor = scanResult.getCursorAsBytes();
    } while (!Arrays.equals("0".getBytes(), cursor));

    assertThat(collectedKeys, hasItems(matchingKey1, matchingKey2));
    assertThat(collectedKeys, not(hasItem(nonMatchingKey)));
  }

  @Test
  public void testScanWithParamsAndType() {
    String stringKey = "user:string:1";
    String listKey = "user:list:1";

    exec(commandObjects.set(stringKey, "value"));
    exec(commandObjects.rpush(listKey, "value1", "value2"));

    ScanParams params = new ScanParams().match("user:*");

    Set<String> collectedKeys = new HashSet<>();

    ScanResult<String> scanResult;
    String cursor = "0";

    do {
      scanResult = exec(commandObjects.scan(cursor, params, "string"));
      collectedKeys.addAll(scanResult.getResult());
      cursor = scanResult.getCursor();
    } while (!"0".equals(scanResult.getCursor()));

    assertThat(collectedKeys, hasItem(stringKey));
    assertThat(collectedKeys, not(hasItem(listKey)));
  }

  @Test
  public void testScanWithParamsAndTypeBinary() {
    byte[] stringKey = "user:string:1".getBytes();
    byte[] listKey = "user:list:1".getBytes();

    exec(commandObjects.set(stringKey, "value".getBytes()));
    exec(commandObjects.rpush(listKey, "value1".getBytes(), "value2".getBytes()));

    ScanParams params = new ScanParams().match("user:*".getBytes());

    Set<byte[]> collectedKeys = new HashSet<>();

    ScanResult<byte[]> scanResult;
    byte[] cursor = "0".getBytes();

    do {
      scanResult = exec(commandObjects.scan(cursor, params, "string".getBytes()));
      collectedKeys.addAll(scanResult.getResult());
      cursor = scanResult.getCursorAsBytes();
    } while (!Arrays.equals("0".getBytes(), cursor));

    assertThat(collectedKeys, hasItem(stringKey));
    assertThat(collectedKeys, not(hasItem(listKey)));
  }

  @Test
  public void testRandomKey() {
    String key1 = "testKey1";
    String key2 = "testKey2";

    exec(commandObjects.set(key1, "value"));
    exec(commandObjects.set(key2, "value"));

    String randomKey = exec(commandObjects.randomKey());

    assertThat(randomKey, anyOf(equalTo(key1), equalTo(key2)));
  }

  @Test
  public void testRandomBinaryKey() {
    byte[] key1 = "testKey1".getBytes();
    byte[] key2 = "testKey2".getBytes();

    exec(commandObjects.set(key1, "value".getBytes()));
    exec(commandObjects.set(key2, "value".getBytes()));

    byte[] randomBinaryKey = exec(commandObjects.randomBinaryKey());

    assertThat(randomBinaryKey, anyOf(equalTo(key1), equalTo(key2)));
  }

}
