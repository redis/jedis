package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.KeyValue;

public class PipeliningBaseGenericCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testCopy() {
    when(commandObjects.copy("srcKey", "dstKey", true)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.copy("srcKey", "dstKey", true);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCopyBinary() {
    byte[] srcKey = "sourceKey".getBytes();
    byte[] dstKey = "destinationKey".getBytes();
    boolean replace = true;

    when(commandObjects.copy(srcKey, dstKey, replace)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.copy(srcKey, dstKey, replace);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDel() {
    when(commandObjects.del("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.del(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.del(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelMultipleKeysBinary() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.del(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDump() {
    when(commandObjects.dump("key")).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.dump("key");

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDumpBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.dump(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.dump(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExists() {
    when(commandObjects.exists("key")).thenReturn(booleanCommandObject);

    Response<Boolean> result = pipeliningBase.exists("key");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(result, is(predefinedResponse));
  }

  @Test
  public void testExistsBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.exists(key)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.exists(key);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExistsMultipleKeys() {
    when(commandObjects.exists("key1", "key2", "key3")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.exists("key1", "key2", "key3");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExistsMultipleKeysBinary() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.exists(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.exists(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpire() {
    when(commandObjects.expire("key", 60)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire("key", 60);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireBinary() {
    byte[] key = "key".getBytes();
    long seconds = 60L;

    when(commandObjects.expire(key, seconds)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire(key, seconds);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireWithExpiryOption() {
    when(commandObjects.expire("key", 60, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire("key", 60, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireWithExpiryOptionBinary() {
    byte[] key = "key".getBytes();
    long seconds = 60L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expire(key, seconds, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire(key, seconds, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAt() {
    int unixTime = 1609459200;

    when(commandObjects.expireAt("key", unixTime)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt("key", unixTime);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtBinary() {
    byte[] key = "key".getBytes();
    long unixTime = 1625097600L;

    when(commandObjects.expireAt(key, unixTime)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt(key, unixTime);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtWithExpiryOption() {
    int unixTime = 1609459200;

    when(commandObjects.expireAt("key", unixTime, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt("key", unixTime, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtWithExpiryOptionBinary() {
    byte[] key = "key".getBytes();
    long unixTime = 1625097600L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expireAt(key, unixTime, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt(key, unixTime, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireTime() {
    when(commandObjects.expireTime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireTime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireTimeBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.expireTime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireTime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testKeys() {
    when(commandObjects.keys("pattern")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.keys("pattern");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testKeysBinary() {
    byte[] pattern = "*".getBytes();

    when(commandObjects.keys(pattern)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.keys(pattern);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrate() {
    when(commandObjects.migrate("host", 6379, "key", 5000)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate("host", 6379, "key", 5000);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateBinary() {
    String host = "localhost";
    int port = 6379;
    byte[] key = "key".getBytes();
    int timeout = 1000;

    when(commandObjects.migrate(host, port, key, timeout)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate(host, port, key, timeout);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateMultipleKeys() {
    MigrateParams params = new MigrateParams();
    String[] keys = { "key1", "key2" };

    when(commandObjects.migrate("host", 6379, 5000, params, keys)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate("host", 6379, 5000, params, keys);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateMultipleKeysBinary() {
    String host = "localhost";
    int port = 6379;
    int timeout = 1000;
    MigrateParams params = MigrateParams.migrateParams().copy().replace();
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.migrate(host, port, timeout, params, key1, key2)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate(host, port, timeout, params, key1, key2);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectEncoding() {
    when(commandObjects.objectEncoding("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.objectEncoding("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectEncodingBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectEncoding(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.objectEncoding(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectFreq() {
    when(commandObjects.objectFreq("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectFreq("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectFreqBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectFreq(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectFreq(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectIdletime() {
    when(commandObjects.objectIdletime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectIdletime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectIdletimeBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectIdletime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectIdletime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectRefcount() {
    when(commandObjects.objectRefcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectRefcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectRefcountBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectRefcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectRefcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPersist() {
    when(commandObjects.persist("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.persist("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPersistBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.persist(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.persist(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpire() {
    when(commandObjects.pexpire("key", 100000)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire("key", 100000);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireBinary() {
    byte[] key = "key".getBytes();
    long milliseconds = 60000L;

    when(commandObjects.pexpire(key, milliseconds)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire(key, milliseconds);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireWithExpiryOption() {
    when(commandObjects.pexpire("key", 100000, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire("key", 100000, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireWithExpiryOptionBinary() {
    byte[] key = "key".getBytes();
    long milliseconds = 60000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpire(key, milliseconds, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire(key, milliseconds, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAt() {
    long millisecondsTimestamp = 1609459200000L;

    when(commandObjects.pexpireAt("key", millisecondsTimestamp)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt("key", millisecondsTimestamp);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtBinary() {
    byte[] key = "key".getBytes();
    long millisecondsTimestamp = 1625097600000L;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt(key, millisecondsTimestamp);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtWithExpiryOption() {
    long millisecondsTimestamp = 1609459200000L;

    when(commandObjects.pexpireAt("key", millisecondsTimestamp, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt("key", millisecondsTimestamp, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtWithExpiryOptionBinary() {
    byte[] key = "key".getBytes();
    long millisecondsTimestamp = 1625097600000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt(key, millisecondsTimestamp, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireTime() {
    when(commandObjects.pexpireTime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireTime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireTimeBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.pexpireTime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireTime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPttl() {
    when(commandObjects.pttl("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pttl("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPttlBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.pttl(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pttl(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRandomKey() {
    when(commandObjects.randomKey()).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.randomKey();

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRandomBinaryKey() {
    when(commandObjects.randomBinaryKey()).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.randomBinaryKey();

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRename() {
    when(commandObjects.rename("oldkey", "newkey")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rename("oldkey", "newkey");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenameBinary() {
    byte[] oldkey = "oldKey".getBytes();
    byte[] newkey = "newKey".getBytes();

    when(commandObjects.rename(oldkey, newkey)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rename(oldkey, newkey);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenamenx() {
    when(commandObjects.renamenx("oldkey", "newkey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.renamenx("oldkey", "newkey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenamenxBinary() {
    byte[] oldkey = "oldKey".getBytes();
    byte[] newkey = "newKey".getBytes();

    when(commandObjects.renamenx(oldkey, newkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.renamenx(oldkey, newkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestore() {
    byte[] serializedValue = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;

    when(commandObjects.restore("key", ttl, serializedValue)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore("key", ttl, serializedValue);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreBinary() {
    byte[] key = "key".getBytes();
    long ttl = 0L;
    byte[] serializedValue = "serialized".getBytes();

    when(commandObjects.restore(key, ttl, serializedValue)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore(key, ttl, serializedValue);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreWithParams() {
    byte[] serializedValue = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;
    RestoreParams params = new RestoreParams();

    when(commandObjects.restore("key", ttl, serializedValue, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore("key", ttl, serializedValue, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreWithParamsBinary() {
    byte[] key = "key".getBytes();
    long ttl = 0L;
    byte[] serializedValue = "serialized".getBytes();
    RestoreParams params = RestoreParams.restoreParams().replace();

    when(commandObjects.restore(key, ttl, serializedValue, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore(key, ttl, serializedValue, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScan() {
    when(commandObjects.scan("0")).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0");

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanBinary() {
    byte[] cursor = "0".getBytes();

    when(commandObjects.scan(cursor)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithParams() {
    ScanParams scanParams = new ScanParams();

    when(commandObjects.scan("0", scanParams)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0", scanParams);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithParamsBinary() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.scan(cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithType() {
    ScanParams scanParams = new ScanParams();

    when(commandObjects.scan("0", scanParams, "type")).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0", scanParams, "type");

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithTypeBinary() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);
    byte[] type = "string".getBytes();

    when(commandObjects.scan(cursor, params, type)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor, params, type);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSort() {
    when(commandObjects.sort("key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sort("key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.sort(key)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sort(key);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortWithParams() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sort("key", sortingParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sort("key", sortingParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortWithParamsBinary() {
    byte[] key = "key".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sort(key, sortingParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sort(key, sortingParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortStore() {
    when(commandObjects.sort("key", "dstKey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort("key", "dstKey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortStoreBinary() {
    byte[] key = "key".getBytes();
    byte[] dstkey = "dstkey".getBytes();

    when(commandObjects.sort(key, dstkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort(key, dstkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortStoreWithParams() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sort("key", sortingParams, "dstKey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort("key", sortingParams, "dstKey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortStoreWithParamsBinary() {
    byte[] key = "key".getBytes();
    byte[] dstkey = "dstkey".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sort(key, sortingParams, dstkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort(key, sortingParams, dstkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortReadonly() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sortReadonly("key", sortingParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sortReadonly("key", sortingParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortReadonlyBinary() {
    byte[] key = "key".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sortReadonly(key, sortingParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sortReadonly(key, sortingParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouch() {
    when(commandObjects.touch("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.touch(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.touch(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchMultipleKeysBinary() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.touch(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTtl() {
    when(commandObjects.ttl("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ttl("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTtlBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.ttl(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ttl(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testType() {
    when(commandObjects.type("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.type("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTypeBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.type(key)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.type(key);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlink() {
    when(commandObjects.unlink("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.unlink(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.unlink(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkMultipleKeysBinary() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.unlink(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitReplicas() {
    int replicas = 2;
    long timeout = 1000L;

    when(commandObjects.waitReplicas("key", replicas, timeout)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.waitReplicas("key", replicas, timeout);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitReplicasBinary() {
    byte[] sampleKey = "sampleKey".getBytes();
    int replicas = 1;
    long timeout = 1000;

    when(commandObjects.waitReplicas(sampleKey, replicas, timeout)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.waitReplicas(sampleKey, replicas, timeout);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitAOF() {
    long numLocal = 1L;
    long numReplicas = 1L;
    long timeout = 1000L;

    when(commandObjects.waitAOF("key", numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);

    Response<KeyValue<Long, Long>> response = pipeliningBase.waitAOF("key", numLocal, numReplicas, timeout);

    assertThat(commands, contains(keyValueLongLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitAOFBinary() {
    byte[] sampleKey = "sampleKey".getBytes();
    long numLocal = 1;
    long numReplicas = 1;
    long timeout = 1000;

    when(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);

    Response<KeyValue<Long, Long>> response = pipeliningBase.waitAOF(sampleKey, numLocal, numReplicas, timeout);

    assertThat(commands, contains(keyValueLongLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
