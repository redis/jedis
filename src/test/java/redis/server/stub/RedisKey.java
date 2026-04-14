package redis.server.stub;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Wrapper for binary-safe Redis keys. Redis keys can contain any byte sequence, not just valid
 * UTF-8 strings. This class provides proper equality semantics and HashMap support for binary keys.
 * Implementation notes: - Immutable wrapper around byte[] - Defensive copying to prevent external
 * modification - Cached hashCode for HashMap performance - Proper equals() using Arrays.equals()
 */
public final class RedisKey {

  private final byte[] key;
  private final int hashCode;

  /**
   * Create a RedisKey from a byte array.
   * @param key the key bytes (will be defensively copied)
   */
  public RedisKey(byte[] key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    this.key = key.clone(); // Defensive copy
    this.hashCode = Arrays.hashCode(key);
  }

  /**
   * Get a copy of the key bytes.
   * @return defensive copy of the key bytes
   */
  public byte[] getBytes() {
    return key.clone(); // Defensive copy
  }

  /**
   * Check equality based on byte content.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RedisKey)) {
      return false;
    }
    RedisKey other = (RedisKey) obj;
    return Arrays.equals(key, other.key);
  }

  /**
   * Cached hash code for HashMap performance.
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * String representation for debugging. Note: May not be valid UTF-8 if key contains binary data.
   */
  @Override
  public String toString() {
    return new String(key, StandardCharsets.UTF_8);
  }

  // ===== Factory Methods =====

  /**
   * Create RedisKey from byte array.
   * @param bytes the key bytes
   * @return new RedisKey instance
   */
  public static RedisKey of(byte[] bytes) {
    return new RedisKey(bytes);
  }

  /**
   * Create RedisKey from UTF-8 string.
   * @param string the key string
   * @return new RedisKey instance
   */
  public static RedisKey of(String string) {
    if (string == null) {
      throw new IllegalArgumentException("String cannot be null");
    }
    return new RedisKey(string.getBytes(StandardCharsets.UTF_8));
  }
}
