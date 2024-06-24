package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashBinaryCommands {

  long hset(byte[] key, byte[] field, byte[] value);

  long hset(byte[] key, Map<byte[], byte[]> hash);

  byte[] hget(byte[] key, byte[] field);

  long hsetnx(byte[] key, byte[] field, byte[] value);

  String hmset(byte[] key, Map<byte[], byte[]> hash);

  List<byte[]> hmget(byte[] key, byte[]... fields);

  long hincrBy(byte[] key, byte[] field, long value);

  double hincrByFloat(byte[] key, byte[] field, double value);

  boolean hexists(byte[] key, byte[] field);

  long hdel(byte[] key, byte[]... field);

  long hlen(byte[] key);

  Set<byte[]> hkeys(byte[] key);

  List<byte[]> hvals(byte[] key);

  Map<byte[], byte[]> hgetAll(byte[] key);

  byte[] hrandfield(byte[] key);

  List<byte[]> hrandfield(byte[] key, long count);

  List<Map.Entry<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count);

  default ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params);

  default ScanResult<byte[]> hscanNoValues(byte[] key, byte[] cursor) {
    return hscanNoValues(key, cursor, new ScanParams());
  }

  ScanResult<byte[]> hscanNoValues(byte[] key, byte[] cursor, ScanParams params);

  long hstrlen(byte[] key, byte[] field);

  /**
   * Set expiry for hash field using relative time to expire (seconds).
   *
   * @param key hash
   * @param seconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpire(byte[] key, long seconds, byte[]... fields);

  /**
   * Set expiry for hash field using relative time to expire (seconds).
   *
   * @param key hash
   * @param seconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpire(byte[] key, long seconds, ExpiryOption condition, byte[]... fields);

  /**
   * Set expiry for hash field using relative time to expire (milliseconds).
   *
   * @param key hash
   * @param milliseconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpire(byte[] key, long milliseconds, byte[]... fields);

  /**
   * Set expiry for hash field using relative time to expire (milliseconds).
   *
   * @param key hash
   * @param milliseconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpire(byte[] key, long milliseconds, ExpiryOption condition, byte[]... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (seconds).
   *
   * @param key hash
   * @param unixTimeSeconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpireAt(byte[] key, long unixTimeSeconds, byte[]... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (seconds).
   *
   * @param key hash
   * @param unixTimeSeconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition, byte[]... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (milliseconds).
   *
   * @param key hash
   * @param unixTimeMillis time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpireAt(byte[] key, long unixTimeMillis, byte[]... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (milliseconds).
   *
   * @param key hash
   * @param unixTimeMillis time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition, byte[]... fields);

  /**
   * Returns the expiration time of a hash field as a Unix timestamp, in seconds.
   *
   * @param key hash
   * @param fields
   * @return Expiration Unix timestamp in seconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hexpireTime(byte[] key, byte[]... fields);

  /**
   * Returns the expiration time of a hash field as a Unix timestamp, in milliseconds.
   *
   * @param key hash
   * @param fields
   * @return Expiration Unix timestamp in milliseconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpexpireTime(byte[] key, byte[]... fields);

  /**
   * Returns the TTL in seconds of a hash field.
   *
   * @param key hash
   * @param fields
   * @return TTL in seconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> httl(byte[] key, byte[]... fields);

  /**
   * Returns the TTL in milliseconds of a hash field.
   *
   * @param key hash
   * @param fields
   * @return TTL in milliseconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpttl(byte[] key, byte[]... fields);

  /**
   * Removes the expiration time for each specified field.
   *
   * @param key hash
   * @param fields
   * @return integer-reply: 1 if the expiration time was removed,
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpersist(byte[] key, byte[]... fields);
}
