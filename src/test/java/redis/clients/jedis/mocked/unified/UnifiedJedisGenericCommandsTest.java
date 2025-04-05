package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ScanIteration;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.KeyValue;

public class UnifiedJedisGenericCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testCopy() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    boolean replace = true;

    when(commandObjects.copy(srcKey, dstKey, replace)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(true);

    boolean result = jedis.copy(srcKey, dstKey, replace);

    assertTrue(result);

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).copy(srcKey, dstKey, replace);
  }

  @Test
  public void testCopyBinary() {
    byte[] srcKey = new byte[]{ 1, 2, 3 };
    byte[] dstKey = new byte[]{ 4, 5, 6 };
    boolean replace = false;

    when(commandObjects.copy(srcKey, dstKey, replace)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(true);

    boolean result = jedis.copy(srcKey, dstKey, replace);

    assertTrue(result);

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).copy(srcKey, dstKey, replace);
  }

  @Test
  public void testDel() {
    String key = "key1";

    when(commandObjects.del(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.del(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).del(key);
  }

  @Test
  public void testDelBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.del(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.del(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).del(key);
  }

  @Test
  public void testDelMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.del(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.del(keys);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).del(keys);
  }

  @Test
  public void testDelMultipleKeysBinary() {
    byte[][] keys = { new byte[]{ 1, 2, 3 }, new byte[]{ 4, 5, 6 } };

    when(commandObjects.del(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    long result = jedis.del(keys);

    assertThat(result, equalTo(2L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).del(keys);
  }

  @Test
  public void testDump() {
    String key = "key1";

    when(commandObjects.dump(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(new byte[]{ 1, 2, 3 });

    byte[] result = jedis.dump(key);

    assertThat(result, equalTo(new byte[]{ 1, 2, 3 }));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).dump(key);
  }

  @Test
  public void testDumpBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.dump(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(new byte[]{ 4, 5, 6 });

    byte[] result = jedis.dump(key);

    assertThat(result, equalTo(new byte[]{ 4, 5, 6 }));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).dump(key);
  }

  @Test
  public void testExists() {
    String key = "mykey";

    when(commandObjects.exists(key)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(true);

    boolean result = jedis.exists(key);

    assertTrue(result);

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).exists(key);
  }

  @Test
  public void testExistsBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.exists(key)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(true);

    boolean result = jedis.exists(key);

    assertTrue(result);

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).exists(key);
  }

  @Test
  public void testExistsMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.exists(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.exists(keys);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).exists(keys);
  }

  @Test
  public void testExistsMultipleKeysBinary() {
    byte[][] keys = { new byte[]{ 1, 2, 3 }, new byte[]{ 4, 5, 6 } };

    when(commandObjects.exists(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    long result = jedis.exists(keys);

    assertThat(result, equalTo(2L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).exists(keys);
  }

  @Test
  public void testExpire() {
    String key = "key1";
    long seconds = 60L;

    when(commandObjects.expire(key, seconds)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expire(key, seconds);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expire(key, seconds);
  }

  @Test
  public void testExpireBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long seconds = 60L;

    when(commandObjects.expire(key, seconds)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expire(key, seconds);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expire(key, seconds);
  }

  @Test
  public void testExpireWithExpiryOption() {
    String key = "key1";
    long seconds = 60L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expire(key, seconds, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expire(key, seconds, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expire(key, seconds, expiryOption);
  }

  @Test
  public void testExpireWithExpiryOptionBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long seconds = 60L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expire(key, seconds, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expire(key, seconds, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expire(key, seconds, expiryOption);
  }

  @Test
  public void testExpireAt() {
    String key = "key1";
    long unixTime = 1633072800L;

    when(commandObjects.expireAt(key, unixTime)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expireAt(key, unixTime);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireAt(key, unixTime);
  }

  @Test
  public void testExpireAtBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long unixTime = 1633072800L;

    when(commandObjects.expireAt(key, unixTime)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expireAt(key, unixTime);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireAt(key, unixTime);
  }

  @Test
  public void testExpireAtWithExpiryOption() {
    String key = "key1";
    long unixTime = 1633072800L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expireAt(key, unixTime, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expireAt(key, unixTime, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireAt(key, unixTime, expiryOption);
  }

  @Test
  public void testExpireAtWithExpiryOptionBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long unixTime = 1633072800L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expireAt(key, unixTime, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.expireAt(key, unixTime, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireAt(key, unixTime, expiryOption);
  }

  @Test
  public void testExpireTime() {
    String key = "key1";

    when(commandObjects.expireTime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1234567890L);

    long result = jedis.expireTime(key);

    assertThat(result, equalTo(1234567890L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireTime(key);
  }

  @Test
  public void testExpireTimeBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.expireTime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1234567890L);

    long result = jedis.expireTime(key);

    assertThat(result, equalTo(1234567890L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).expireTime(key);
  }

  @Test
  public void testKeys() {
    String pattern = "*";
    Set<String> expectedKeys = new HashSet<>(Arrays.asList("key1", "key2", "key3"));

    when(commandObjects.keys(pattern)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedKeys);

    Set<String> result = jedis.keys(pattern);

    assertThat(result, equalTo(expectedKeys));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).keys(pattern);
  }

  @Test
  public void testKeysBinary() {
    byte[] pattern = "key*".getBytes();
    Set<byte[]> expectedKeys = new HashSet<>(Arrays.asList("key1".getBytes(), "key2".getBytes()));

    when(commandObjects.keys(pattern)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedKeys);

    Set<byte[]> result = jedis.keys(pattern);

    assertThat(result, equalTo(expectedKeys));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).keys(pattern);
  }

  @Test
  public void testMigrate() {
    String host = "destinationHost";
    int port = 6379;
    String key = "myKey";
    int timeout = 5000;
    String expectedResponse = "OK";

    when(commandObjects.migrate(host, port, key, timeout)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.migrate(host, port, key, timeout);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).migrate(host, port, key, timeout);
  }

  @Test
  public void testMigrateBinary() {
    String host = "destinationHost";
    int port = 6379;
    byte[] key = "myKey".getBytes();
    int timeout = 5000;
    String expectedResponse = "OK";

    when(commandObjects.migrate(host, port, key, timeout)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.migrate(host, port, key, timeout);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).migrate(host, port, key, timeout);
  }

  @Test
  public void testMigrateMultipleKeys() {
    String host = "destinationHost";
    int port = 6379;
    int timeout = 5000;
    MigrateParams params = new MigrateParams();
    String[] keys = { "key1", "key2" };
    String expectedResponse = "OK";

    when(commandObjects.migrate(host, port, timeout, params, keys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.migrate(host, port, timeout, params, keys);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).migrate(host, port, timeout, params, keys);
  }

  @Test
  public void testMigrateMultipleKeysBinary() {
    String host = "destinationHost";
    int port = 6379;
    int timeout = 5000;
    MigrateParams params = new MigrateParams();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };
    String expectedResponse = "OK";

    when(commandObjects.migrate(host, port, timeout, params, keys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.migrate(host, port, timeout, params, keys);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).migrate(host, port, timeout, params, keys);
  }

  @Test
  public void testObjectEncoding() {
    String key = "myKey";
    String expectedEncoding = "ziplist";

    when(commandObjects.objectEncoding(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedEncoding);

    String result = jedis.objectEncoding(key);

    assertThat(result, equalTo(expectedEncoding));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).objectEncoding(key);
  }

  @Test
  public void testObjectEncodingBinary() {
    byte[] key = "myKey".getBytes();
    byte[] expectedEncoding = "ziplist".getBytes();

    when(commandObjects.objectEncoding(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedEncoding);

    byte[] result = jedis.objectEncoding(key);

    assertThat(result, equalTo(expectedEncoding));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).objectEncoding(key);
  }

  @Test
  public void testObjectFreq() {
    String key = "myKey";
    Long expectedFreq = 10L;

    when(commandObjects.objectFreq(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedFreq);

    Long result = jedis.objectFreq(key);

    assertThat(result, equalTo(expectedFreq));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectFreq(key);
  }

  @Test
  public void testObjectFreqBinary() {
    byte[] key = "myKey".getBytes();
    Long expectedFreq = 10L;

    when(commandObjects.objectFreq(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedFreq);

    Long result = jedis.objectFreq(key);

    assertThat(result, equalTo(expectedFreq));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectFreq(key);
  }

  @Test
  public void testObjectIdletime() {
    String key = "myKey";
    Long expectedIdletime = 3600L;

    when(commandObjects.objectIdletime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedIdletime);

    Long result = jedis.objectIdletime(key);

    assertThat(result, equalTo(expectedIdletime));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectIdletime(key);
  }

  @Test
  public void testObjectIdletimeBinary() {
    byte[] key = "myKey".getBytes();
    Long expectedIdletime = 3600L;

    when(commandObjects.objectIdletime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedIdletime);

    Long result = jedis.objectIdletime(key);

    assertThat(result, equalTo(expectedIdletime));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectIdletime(key);
  }

  @Test
  public void testObjectRefcount() {
    String key = "myKey";
    Long expectedRefcount = 42L;

    when(commandObjects.objectRefcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRefcount);

    Long result = jedis.objectRefcount(key);

    assertThat(result, equalTo(expectedRefcount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectRefcount(key);
  }

  @Test
  public void testObjectRefcountBinary() {
    byte[] key = "myKey".getBytes();
    Long expectedRefcount = 42L;

    when(commandObjects.objectRefcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRefcount);

    Long result = jedis.objectRefcount(key);

    assertThat(result, equalTo(expectedRefcount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).objectRefcount(key);
  }

  @Test
  public void testPersist() {
    String key = "key1";

    when(commandObjects.persist(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.persist(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).persist(key);
  }

  @Test
  public void testPersistBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.persist(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.persist(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).persist(key);
  }

  @Test
  public void testPexpire() {
    String key = "key1";
    long milliseconds = 1000L;

    when(commandObjects.pexpire(key, milliseconds)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpire(key, milliseconds);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpire(key, milliseconds);
  }

  @Test
  public void testPexpireBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long milliseconds = 1000L;

    when(commandObjects.pexpire(key, milliseconds)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpire(key, milliseconds);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpire(key, milliseconds);
  }

  @Test
  public void testPexpireWithExpiryOption() {
    String key = "key1";
    long milliseconds = 1000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpire(key, milliseconds, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpire(key, milliseconds, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpire(key, milliseconds, expiryOption);
  }

  @Test
  public void testPexpireWithExpiryOptionBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long milliseconds = 1000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpire(key, milliseconds, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpire(key, milliseconds, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpire(key, milliseconds, expiryOption);
  }

  @Test
  public void testPexpireAt() {
    String key = "key1";
    long millisecondsTimestamp = 1633072800123L;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpireAt(key, millisecondsTimestamp);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireAt(key, millisecondsTimestamp);
  }

  @Test
  public void testPexpireAtBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long millisecondsTimestamp = 1633072800123L;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpireAt(key, millisecondsTimestamp);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireAt(key, millisecondsTimestamp);
  }

  @Test
  public void testPexpireAtWithExpiryOption() {
    String key = "key1";
    long millisecondsTimestamp = 1633072800123L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpireAt(key, millisecondsTimestamp, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireAt(key, millisecondsTimestamp, expiryOption);
  }

  @Test
  public void testPexpireAtWithExpiryOptionBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long millisecondsTimestamp = 1633072800123L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.pexpireAt(key, millisecondsTimestamp, expiryOption);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireAt(key, millisecondsTimestamp, expiryOption);
  }

  @Test
  public void testPexpireTime() {
    String key = "key1";

    when(commandObjects.pexpireTime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1234567890123L);

    long result = jedis.pexpireTime(key);

    assertThat(result, equalTo(1234567890123L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireTime(key);
  }

  @Test
  public void testPexpireTimeBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.pexpireTime(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1234567890123L);

    long result = jedis.pexpireTime(key);

    assertThat(result, equalTo(1234567890123L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pexpireTime(key);
  }

  @Test
  public void testPttl() {
    String key = "key1";

    when(commandObjects.pttl(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(120000L);

    long result = jedis.pttl(key);

    assertThat(result, equalTo(120000L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pttl(key);
  }

  @Test
  public void testPttlBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.pttl(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(120000L);

    long result = jedis.pttl(key);

    assertThat(result, equalTo(120000L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pttl(key);
  }

  @Test
  public void testRandomKey() {
    String expectedKey = "randomKey";

    when(commandObjects.randomKey()).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedKey);

    String result = jedis.randomKey();

    assertThat(result, equalTo(expectedKey));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).randomKey();
  }

  @Test
  public void testRandomBinaryKey() {
    byte[] expectedKey = "randomKey".getBytes();

    when(commandObjects.randomBinaryKey()).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedKey);

    byte[] result = jedis.randomBinaryKey();

    assertThat(result, equalTo(expectedKey));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).randomBinaryKey();
  }

  @Test
  public void testRename() {
    String oldkey = "oldKey";
    String newkey = "newKey";
    String expectedStatus = "OK";

    when(commandObjects.rename(oldkey, newkey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.rename(oldkey, newkey);

    assertEquals(expectedStatus, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).rename(oldkey, newkey);
  }

  @Test
  public void testRenameBinary() {
    byte[] oldkey = new byte[]{ 1, 2, 3 };
    byte[] newkey = new byte[]{ 4, 5, 6 };
    String expectedStatus = "OK";

    when(commandObjects.rename(oldkey, newkey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.rename(oldkey, newkey);

    assertEquals(expectedStatus, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).rename(oldkey, newkey);
  }

  @Test
  public void testRenamenx() {
    String oldkey = "oldKey";
    String newkey = "newKey";
    long expected = 1L;

    when(commandObjects.renamenx(oldkey, newkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.renamenx(oldkey, newkey);

    assertEquals(expected, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).renamenx(oldkey, newkey);
  }

  @Test
  public void testRenamenxBinary() {
    byte[] oldkey = new byte[]{ 1, 2, 3 };
    byte[] newkey = new byte[]{ 4, 5, 6 };
    long expected = 1L;

    when(commandObjects.renamenx(oldkey, newkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.renamenx(oldkey, newkey);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).renamenx(oldkey, newkey);
  }

  @Test
  public void testRestore() {
    String key = "key1";
    long ttl = 0L;
    byte[] serializedValue = new byte[]{ 1, 2, 3 };

    when(commandObjects.restore(key, ttl, serializedValue)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.restore(key, ttl, serializedValue);

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).restore(key, ttl, serializedValue);
  }

  @Test
  public void testRestoreBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;
    byte[] serializedValue = new byte[]{ 4, 5, 6 };

    when(commandObjects.restore(key, ttl, serializedValue)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.restore(key, ttl, serializedValue);

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).restore(key, ttl, serializedValue);
  }

  @Test
  public void testRestoreWithParams() {
    String key = "key1";
    long ttl = 0L;
    byte[] serializedValue = new byte[]{ 1, 2, 3 };
    RestoreParams params = new RestoreParams();

    when(commandObjects.restore(key, ttl, serializedValue, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.restore(key, ttl, serializedValue, params);

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).restore(key, ttl, serializedValue, params);

  }

  @Test
  public void testRestoreWithParamsBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;
    byte[] serializedValue = new byte[]{ 4, 5, 6 };
    RestoreParams params = new RestoreParams();

    when(commandObjects.restore(key, ttl, serializedValue, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.restore(key, ttl, serializedValue, params);

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).restore(key, ttl, serializedValue, params);
  }

  @Test
  public void testScan() {
    String cursor = "0";
    ScanResult<String> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1", "key2"));

    when(commandObjects.scan(cursor)).thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<String> result = jedis.scan(cursor);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).scan(cursor);
  }

  @Test
  public void testScanBinary() {
    byte[] cursor = "0".getBytes();
    ScanResult<byte[]> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1".getBytes(), "key2".getBytes()));

    when(commandObjects.scan(cursor)).thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<byte[]> result = jedis.scan(cursor);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).scan(cursor);
  }

  @Test
  public void testScanWithParams() {
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    ScanResult<String> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1", "key2"));

    when(commandObjects.scan(cursor, params)).thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<String> result = jedis.scan(cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).scan(cursor, params);
  }

  @Test
  public void testScanWithParamsBinary() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*".getBytes()).count(10);
    ScanResult<byte[]> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1".getBytes(), "key2".getBytes()));

    when(commandObjects.scan(cursor, params)).thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<byte[]> result = jedis.scan(cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).scan(cursor, params);
  }

  @Test
  public void testScanWithType() {
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    String type = "hash";
    ScanResult<String> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1", "key2"));

    when(commandObjects.scan(cursor, params, type)).thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<String> result = jedis.scan(cursor, params, type);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).scan(cursor, params, type);
  }

  @Test
  public void testScanWithTypeBinary() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*".getBytes()).count(10);
    byte[] type = "string".getBytes();
    ScanResult<byte[]> expectedScanResult = new ScanResult<>(cursor, Arrays.asList("key1".getBytes(), "key2".getBytes()));

    when(commandObjects.scan(cursor, params, type)).thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<byte[]> result = jedis.scan(cursor, params, type);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).scan(cursor, params, type);
  }

  @Test
  public void testScanIteration() {
    String cursor = "0";
    String key1 = "key1";
    String key2 = "key2";

    Connection connection = mock(Connection.class);
    when(connection.executeCommand(any(CommandArguments.class)))
        .thenReturn(Arrays.asList(cursor.getBytes(), Arrays.asList(key1.getBytes(), key2.getBytes())));

    when(connectionProvider.getConnectionMap()).thenAnswer(new Answer<Map<?, ?>>() {
      @Override
      public Map<?, ?> answer(InvocationOnMock invocationOnMock) {
        return Collections.singletonMap("c", connection);
      }
    });

    ScanIteration result = jedis.scanIteration(10, "prefix:*");

    ScanResult<String> batch = result.nextBatch();
    assertThat(batch.getCursor(), equalTo(cursor));
    assertThat(batch.getResult(), contains(key1, key2));

    verify(connectionProvider).getConnectionMap();
  }

  @Test
  public void testScanIterationWithType() {
    String cursor = "0";
    String key1 = "key1";
    String key2 = "key2";

    Connection connection = mock(Connection.class);
    when(connection.executeCommand(any(CommandArguments.class)))
        .thenReturn(Arrays.asList(cursor.getBytes(), Arrays.asList(key1.getBytes(), key2.getBytes())));

    when(connectionProvider.getConnectionMap()).thenAnswer(new Answer<Map<?, ?>>() {
      @Override
      public Map<?, ?> answer(InvocationOnMock invocationOnMock) {
        return Collections.singletonMap("c", connection);
      }
    });

    ScanIteration result = jedis.scanIteration(10, "prefix:*", "zset");

    ScanResult<String> batch = result.nextBatch();
    assertThat(batch.getCursor(), equalTo(cursor));
    assertThat(batch.getResult(), contains(key1, key2));

    verify(connectionProvider).getConnectionMap();
  }

  @Test
  public void testSort() {
    String key = "key1";
    List<String> expected = Arrays.asList("one", "two", "three");

    when(commandObjects.sort(key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expected);

    List<String> result = jedis.sort(key);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).sort(key);
  }

  @Test
  public void testSortBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    List<byte[]> expected = Arrays.asList(new byte[]{ 4 }, new byte[]{ 5 }, new byte[]{ 6 });

    when(commandObjects.sort(key)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expected);

    List<byte[]> result = jedis.sort(key);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).sort(key);
  }

  @Test
  public void testSortWithParams() {
    String key = "key1";
    SortingParams sortingParams = new SortingParams().asc();
    List<String> expected = Arrays.asList("one", "three", "two");

    when(commandObjects.sort(key, sortingParams)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expected);

    List<String> result = jedis.sort(key, sortingParams);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).sort(key, sortingParams);
  }

  @Test
  public void testSortWithParamsBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    SortingParams sortingParams = new SortingParams().asc();
    List<byte[]> expected = Arrays.asList(new byte[]{ 4 }, new byte[]{ 6 }, new byte[]{ 5 });

    when(commandObjects.sort(key, sortingParams)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expected);

    List<byte[]> result = jedis.sort(key, sortingParams);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).sort(key, sortingParams);
  }

  @Test
  public void testSortStore() {
    String key = "key1";
    String dstkey = "resultKey";

    when(commandObjects.sort(key, dstkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.sort(key, dstkey);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sort(key, dstkey);
  }

  @Test
  public void testSortStoreBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    byte[] dstkey = new byte[]{ 7, 8, 9 };

    when(commandObjects.sort(key, dstkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.sort(key, dstkey);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sort(key, dstkey);
  }

  @Test
  public void testSortStoreWithParams() {
    String key = "key1";
    SortingParams sortingParams = new SortingParams().asc();
    String dstkey = "resultKey";

    when(commandObjects.sort(key, sortingParams, dstkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.sort(key, sortingParams, dstkey);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sort(key, sortingParams, dstkey);
  }

  @Test
  public void testSortStoreWithParamsBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    SortingParams sortingParams = new SortingParams().asc();
    byte[] dstkey = new byte[]{ 7, 8, 9 };

    when(commandObjects.sort(key, sortingParams, dstkey)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.sort(key, sortingParams, dstkey);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sort(key, sortingParams, dstkey);
  }

  @Test
  public void testSortReadonly() {
    String key = "key1";
    SortingParams sortingParams = new SortingParams().asc();
    List<String> expected = Arrays.asList("one", "three", "two");

    when(commandObjects.sortReadonly(key, sortingParams)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expected);

    List<String> result = jedis.sortReadonly(key, sortingParams);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).sortReadonly(key, sortingParams);
  }

  @Test
  public void testSortReadonlyBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    SortingParams sortingParams = new SortingParams().asc();
    List<byte[]> expected = Arrays.asList(new byte[]{ 4 }, new byte[]{ 6 }, new byte[]{ 5 });

    when(commandObjects.sortReadonly(key, sortingParams)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expected);

    List<byte[]> result = jedis.sortReadonly(key, sortingParams);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).sortReadonly(key, sortingParams);
  }

  @Test
  public void testTouch() {
    String key = "key1";

    when(commandObjects.touch(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.touch(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).touch(key);
  }

  @Test
  public void testTouchBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.touch(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.touch(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).touch(key);
  }

  @Test
  public void testTouchMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.touch(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.touch(keys);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).touch(keys);
  }

  @Test
  public void testTouchMultipleKeysBinary() {
    byte[][] keys = { new byte[]{ 1, 2, 3 }, new byte[]{ 4, 5, 6 } };

    when(commandObjects.touch(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    long result = jedis.touch(keys);

    assertThat(result, equalTo(2L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).touch(keys);
  }

  @Test
  public void testTtl() {
    String key = "key1";

    when(commandObjects.ttl(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(120L);

    long result = jedis.ttl(key);

    assertThat(result, equalTo(120L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ttl(key);
  }

  @Test
  public void testTtlBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.ttl(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(120L);

    long result = jedis.ttl(key);

    assertThat(result, equalTo(120L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ttl(key);
  }

  @Test
  public void testType() {
    String key = "key1";

    when(commandObjects.type(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("string");

    String result = jedis.type(key);

    assertThat(result, equalTo("string"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).type(key);
  }

  @Test
  public void testTypeBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.type(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("string");

    String result = jedis.type(key);

    assertThat(result, equalTo("string"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).type(key);
  }

  @Test
  public void testUnlink() {
    String key = "key1";

    when(commandObjects.unlink(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.unlink(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).unlink(key);
  }

  @Test
  public void testUnlinkBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };

    when(commandObjects.unlink(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.unlink(key);

    assertThat(result, equalTo(1L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).unlink(key);
  }

  @Test
  public void testUnlinkMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.unlink(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(3L);

    long result = jedis.unlink(keys);

    assertThat(result, equalTo(3L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).unlink(keys);
  }

  @Test
  public void testUnlinkMultipleKeysBinary() {
    byte[][] keys = { new byte[]{ 1, 2, 3 }, new byte[]{ 4, 5, 6 } };

    when(commandObjects.unlink(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(2L);

    long result = jedis.unlink(keys);

    assertThat(result, equalTo(2L));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).unlink(keys);
  }

  @Test
  public void testWaitReplicas() {
    String sampleKey = "myKey";
    int replicas = 2;
    long timeout = 10000L;
    long expectedReplicaCount = 2L;

    when(commandObjects.waitReplicas(sampleKey, replicas, timeout)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedReplicaCount);

    long result = jedis.waitReplicas(sampleKey, replicas, timeout);

    assertThat(result, equalTo(expectedReplicaCount));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).waitReplicas(sampleKey, replicas, timeout);
  }

  @Test
  public void testWaitReplicasBinary() {
    byte[] sampleKey = "myKey".getBytes();
    int replicas = 2;
    long timeout = 10000L;
    long expectedReplicaCount = 2L;

    when(commandObjects.waitReplicas(sampleKey, replicas, timeout)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedReplicaCount);

    long result = jedis.waitReplicas(sampleKey, replicas, timeout);

    assertThat(result, equalTo(expectedReplicaCount));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).waitReplicas(sampleKey, replicas, timeout);
  }

  @Test
  public void testWaitAOF() {
    String sampleKey = "myKey";
    long numLocal = 1L;
    long numReplicas = 2L;
    long timeout = 10000L;
    KeyValue<Long, Long> expectedResponse = new KeyValue<>(numLocal, numReplicas);

    when(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);
    when(commandExecutor.executeCommand(keyValueLongLongCommandObject)).thenReturn(expectedResponse);

    KeyValue<Long, Long> result = jedis.waitAOF(sampleKey, numLocal, numReplicas, timeout);

    assertThat(result, equalTo(expectedResponse));
    verify(commandExecutor).executeCommand(keyValueLongLongCommandObject);
    verify(commandObjects).waitAOF(sampleKey, numLocal, numReplicas, timeout);
  }

  @Test
  public void testWaitAOFBinary() {
    byte[] sampleKey = "myKey".getBytes();
    long numLocal = 1L;
    long numReplicas = 2L;
    long timeout = 10000L;
    KeyValue<Long, Long> expectedResponse = new KeyValue<>(numLocal, numReplicas);

    when(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);
    when(commandExecutor.executeCommand(keyValueLongLongCommandObject)).thenReturn(expectedResponse);

    KeyValue<Long, Long> result = jedis.waitAOF(sampleKey, numLocal, numReplicas, timeout);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(keyValueLongLongCommandObject);
    verify(commandObjects).waitAOF(sampleKey, numLocal, numReplicas, timeout);
  }

}
