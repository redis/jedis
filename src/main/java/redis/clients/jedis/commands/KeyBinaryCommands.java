package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;

public interface KeyBinaryCommands {

  boolean exists(byte[] key);

  long exists(byte[]... keys);

  long persist(byte[] key);

  String type(byte[] key);

  byte[] dump(byte[] key);

  String restore(byte[] key, long ttl, byte[] serializedValue);

  String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params);

  long expire(byte[] key, long seconds);

  long expire(byte[] key, long seconds, ExpiryOption expiryOption);

  long pexpire(byte[] key, long milliseconds);

  long pexpire(byte[] key, long milliseconds, ExpiryOption expiryOption);

  long expireTime(byte[] key);

  long pexpireTime(byte[] key);

  long expireAt(byte[] key, long unixTime);

  long expireAt(byte[] key, long unixTime, ExpiryOption expiryOption);

  long pexpireAt(byte[] key, long millisecondsTimestamp);

  long pexpireAt(byte[] key, long millisecondsTimestamp, ExpiryOption expiryOption);

  long ttl(byte[] key);

  long pttl(byte[] key);

  long touch(byte[] key);

  long touch(byte[]... keys);

  List<byte[]> sort(byte[] key);

  List<byte[]> sort(byte[] key, SortingParams sortingParams);

  long del(byte[] key);

  long del(byte[]... keys);

  long unlink(byte[] key);

  long unlink(byte[]... keys);

  boolean copy(byte[] srcKey, byte[] dstKey, boolean replace);

  String rename(byte[] oldkey, byte[] newkey);

  long renamenx(byte[] oldkey, byte[] newkey);

  long sort(byte[] key, SortingParams sortingParams, byte[] dstkey);

  long sort(byte[] key, byte[] dstkey);

  List<byte[]> sortReadonly(byte[] key, SortingParams sortingParams);

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);

  Long objectRefcount(byte[] key);

  byte[] objectEncoding(byte[] key);

  Long objectIdletime(byte[] key);

  Long objectFreq(byte[] key);

  String migrate(String host, int port, byte[] key, int timeout);

  String migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys);

  Set<byte[]> keys(byte[] pattern);

  ScanResult<byte[]> scan(byte[] cursor);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type);

  byte[] randomBinaryKey();

}
