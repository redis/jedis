package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;

public interface KeyPipelineBinaryCommands {

  Response<Boolean> exists(byte[] key);

  Response<Long> exists(byte[]... keys);

  Response<Long> persist(byte[] key);

  Response<String> type(byte[] key);

  Response<byte[]> dump(byte[] key);

  Response<String> restore(byte[] key, long ttl, byte[] serializedValue);

  Response<String> restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params);

  Response<Long> expire(byte[] key, long seconds);

  Response<Long> expire(byte[] key, long seconds, ExpiryOption expiryOption);

  Response<Long> pexpire(byte[] key, long milliseconds);

  Response<Long> pexpire(byte[] key, long milliseconds, ExpiryOption expiryOption);

  Response<Long> expireTime(byte[] key);

  Response<Long> pexpireTime(byte[] key);

  Response<Long> expireAt(byte[] key, long unixTime);

  Response<Long> expireAt(byte[] key, long unixTime, ExpiryOption expiryOption);

  Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp);

  Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp, ExpiryOption expiryOption);

  Response<Long> ttl(byte[] key);

  Response<Long> pttl(byte[] key);

  Response<Long> touch(byte[] key);

  Response<Long> touch(byte[]... keys);

  Response<List<byte[]>> sort(byte[] key);

  Response<List<byte[]>> sort(byte[] key, SortingParams sortingParams);

  Response<List<byte[]>> sortReadonly(byte[] key, SortingParams sortingParams);

  Response<Long> del(byte[] key);

  Response<Long> del(byte[]... keys);

  Response<Long> unlink(byte[] key);

  Response<Long> unlink(byte[]... keys);

  Response<Boolean> copy(byte[] srcKey, byte[] dstKey, boolean replace);

  Response<String> rename(byte[] oldkey, byte[] newkey);

  Response<Long> renamenx(byte[] oldkey, byte[] newkey);

  Response<Long> sort(byte[] key, SortingParams sortingParams, byte[] dstkey);

  Response<Long> sort(byte[] key, byte[] dstkey);

  Response<Long> memoryUsage(byte[] key);

  Response<Long> memoryUsage(byte[] key, int samples);

  Response<Long> objectRefcount(byte[] key);

  Response<byte[]> objectEncoding(byte[] key);

  Response<Long> objectIdletime(byte[] key);

  Response<Long> objectFreq(byte[] key);

  Response<String> migrate(String host, int port, byte[] key, int timeout);

  Response<String> migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys);

  Response<Set<byte[]>> keys(byte[] pattern);

  Response<ScanResult<byte[]>> scan(byte[] cursor);

  Response<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params);

  Response<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type);

  Response<byte[]> randomBinaryKey();

}
