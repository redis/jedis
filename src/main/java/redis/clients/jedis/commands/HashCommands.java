package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.HGetExParams;
import redis.clients.jedis.params.HSetExParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashCommands {

  long hset(String key, String field, String value);

  long hset(String key, Map<String, String> hash);

  /**
   * Sets the specified fields in the hash stored at key to the specified values with additional parameters,
   * and optionally set their expiration. Use `HSetExParams` object to specify expiration parameters.
   * This command can overwrite any existing fields in the hash.
   * If key does not exist, a new key holding a hash is created.
   * 
   * @param key the key of the hash
   * @param params the parameters for the HSETEX command
   * @param field the field in the hash
   * @param value the value to set
   * @return 0 if no fields were set, 1 if all the fields were set 
   * 
   * @see HSetExParams
   */  
  long hsetex(String key, HSetExParams params, String field, String value);

  /**
   * Sets the specified fields in the hash stored at key to the specified values with additional parameters,
   * and optionally set their expiration. Use `HSetExParams` object to specify expiration parameters.
   * This command can overwrite any existing fields in the hash.
   * If key does not exist, a new key holding a hash is created.
   * 
   * @param key the key of the hash
   * @param params the parameters for the HSETEX command
   * @param hash the map containing field-value pairs to set in the hash
   * @return 0 if no fields were set, 1 if all the fields were set 
   * 
   * @see HSetExParams
   */
  long hsetex(String key, HSetExParams params, Map<String, String> hash);

  String hget(String key, String field);
    
  /**
   * Retrieves the values associated with the specified fields in a hash stored at the given key 
   * and optionally sets their expiration. Use `HGetExParams` object to specify expiration parameters.
   *
   * @param key the key of the hash
   * @param params additional parameters for the HGETEX command
   * @param fields the fields whose values are to be retrieved
   * @return a list of the value associated with each field or nil if the field doesn’t exist.
   * 
   * @see HGetExParams
   */
  List<String> hgetex(String key, HGetExParams params, String... fields);

  /**
   * Retrieves the values associated with the specified fields in the hash stored at the given key
   * and then deletes those fields from the hash.
   *
   * @param key the key of the hash
   * @param fields the fields whose values are to be retrieved and then deleted
   * @return a list of values associated with the specified fields before they were deleted
   */
  List<String> hgetdel(String key, String... fields);
  
  long hsetnx(String key, String field, String value);

  String hmset(String key, Map<String, String> hash);

  List<String> hmget(String key, String... fields);

  long hincrBy(String key, String field, long value);

  double hincrByFloat(String key, String field, double value);

  boolean hexists(String key, String field);

  long hdel(String key, String... field);

  long hlen(String key);

  Set<String> hkeys(String key);

  List<String> hvals(String key);

  Map<String, String> hgetAll(String key);

  String hrandfield(String key);

  List<String> hrandfield(String key, long count);

  List<Map.Entry<String, String>> hrandfieldWithValues(String key, long count);

  default ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

  default ScanResult<String> hscanNoValues(String key, String cursor) {
    return hscanNoValues(key, cursor, new ScanParams());
  }

  ScanResult<String> hscanNoValues(String key, String cursor, ScanParams params);

  long hstrlen(String key, String field);

  /**
   * Set expiry for hash field using relative time to expire (seconds).
   *
   * @param key hash
   * @param seconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpire(String key, long seconds, String... fields);

  /**
   * Set expiry for hash field using relative time to expire (seconds).
   *
   * @param key hash
   * @param seconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpire(String key, long seconds, ExpiryOption condition, String... fields);

  /**
   * Set expiry for hash field using relative time to expire (milliseconds).
   *
   * @param key hash
   * @param milliseconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpire(String key, long milliseconds, String... fields);

  /**
   * Set expiry for hash field using relative time to expire (milliseconds).
   *
   * @param key hash
   * @param milliseconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpire(String key, long milliseconds, ExpiryOption condition, String... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (seconds).
   *
   * @param key hash
   * @param unixTimeSeconds time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpireAt(String key, long unixTimeSeconds, String... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (seconds).
   *
   * @param key hash
   * @param unixTimeSeconds time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (milliseconds).
   *
   * @param key hash
   * @param unixTimeMillis time to expire
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpireAt(String key, long unixTimeMillis, String... fields);

  /**
   * Set expiry for hash field using an absolute Unix timestamp (milliseconds).
   *
   * @param key hash
   * @param unixTimeMillis time to expire
   * @param condition can be NX, XX, GT or LT
   * @param fields
   * @return integer-reply: 1 if the timeout was set, 0 otherwise
   */
  List<Long> hpexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... fields);

  /**
   * Returns the expiration time of a hash field as a Unix timestamp, in seconds.
   *
   * @param key hash
   * @param fields
   * @return Expiration Unix timestamp in seconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hexpireTime(String key, String... fields);

  /**
   * Returns the expiration time of a hash field as a Unix timestamp, in milliseconds.
   *
   * @param key hash
   * @param fields
   * @return Expiration Unix timestamp in milliseconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpexpireTime(String key, String... fields);

  /**
   * Returns the TTL in seconds of a hash field.
   *
   * @param key hash
   * @param fields
   * @return TTL in seconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> httl(String key, String... fields);

  /**
   * Returns the TTL in milliseconds of a hash field.
   *
   * @param key hash
   * @param fields
   * @return TTL in milliseconds;
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpttl(String key, String... fields);

  /**
   * Removes the expiration time for each specified field.
   *
   * @param key hash
   * @param fields
   * @return integer-reply: 1 if the expiration time was removed,
   *         or -1 if the field exists but has no associated expire or -2 if the field does not exist.
   */
  List<Long> hpersist(String key, String... fields);
}
