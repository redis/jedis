package redis.server.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Primitive storage layer for Redis data. Binary-safe keys using RedisKey. Single-threaded access -
 * accessed only from single command executor thread.
 * <p>
 * <b>MVP Limitations</b>: This initial implementation does NOT support key expiration (TTL).
 * Commands like EXPIRE, EXPIREAT, TTL are not implemented. Keys persist until explicitly deleted.
 */
public class RedisDataStore {

  // Per-database storage: db -> (key -> value)
  // Keys are binary-safe (byte[] wrapped in RedisKey)
  private final Map<Integer, HashMap<RedisKey, StoredValue>> databases = new HashMap<>();

  // ===== CORE STORAGE PRIMITIVES =====

  /**
   * Get value by key. Returns null if key doesn't exist.
   * @param db database number
   * @param key the key
   * @return the stored value, or null if not found
   */
  public StoredValue get(int db, RedisKey key) {
    return getDatabaseMap(db).get(key);
  }

  /**
   * Set value by key.
   * @param db database number
   * @param key the key
   * @param value the value to store
   */
  public void set(int db, RedisKey key, StoredValue value) {
    getDatabaseMap(db).put(key, value);
  }

  /**
   * Delete key. Returns true if key existed.
   * @param db database number
   * @param key the key
   * @return true if key was deleted, false if it didn't exist
   */
  public boolean delete(int db, RedisKey key) {
    return getDatabaseMap(db).remove(key) != null;
  }

  /**
   * Check if key exists.
   * @param db database number
   * @param key the key
   * @return true if key exists
   */
  public boolean exists(int db, RedisKey key) {
    return getDatabaseMap(db).containsKey(key);
  }

  /**
   * Get all keys in database (for iteration). NOTE: Commands must filter these (e.g., KEYS pattern
   * matching).
   * @param db database number
   * @return set of all keys
   */
  public Set<RedisKey> getAllKeys(int db) {
    return getDatabaseMap(db).keySet();
  }

  // ===== DATABASE PRIMITIVES =====

  /**
   * Get number of keys in database.
   * @param db database number
   * @return number of keys
   */
  public long size(int db) {
    return getDatabaseMap(db).size();
  }

  /**
   * Clear all keys in database.
   * @param db database number
   */
  public void clear(int db) {
    HashMap<RedisKey, StoredValue> dbMap = databases.get(db);
    if (dbMap != null) {
      dbMap.clear();
    }
  }

  /**
   * Clear all databases.
   */
  public void clearAll() {
    databases.clear();
  }

  // ===== INTERNAL HELPERS =====

  /**
   * Get the HashMap for a specific database, creating it if needed.
   */
  private HashMap<RedisKey, StoredValue> getDatabaseMap(int db) {
    return databases.computeIfAbsent(db, k -> new HashMap<>());
  }
}
