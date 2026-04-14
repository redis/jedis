package redis.server.stub;

import java.nio.charset.StandardCharsets;

/**
 * Wrapper for Redis values with type information. Redis supports multiple value types (string,
 * list, set, hash, etc.). This class wraps the value data and tracks its type. For MVP, we only
 * support STRING type.
 */
public class StoredValue {

  /**
   * Redis value types.
   */
  public enum ValueType {
    STRING // , LIST, SET, HASH, ZSET
  }

  private final byte[] value;
  private final ValueType type;

  /**
   * Create a StoredValue.
   * @param value the value bytes (will be defensively copied)
   * @param type the value type
   */
  private StoredValue(byte[] value, ValueType type) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    this.value = value.clone(); // Defensive copy
    this.type = type;
  }

  /**
   * Get a copy of the value bytes.
   * @return defensive copy of the value bytes
   */
  public byte[] getBytes() {
    return value.clone(); // Defensive copy
  }

  /**
   * Get the value as a UTF-8 string. Note: May not be valid UTF-8 if value contains binary data.
   * @return string representation
   */
  public String asString() {
    return new String(value, StandardCharsets.UTF_8);
  }

  /**
   * Get the value type.
   * @return the type
   */
  public ValueType getType() {
    return type;
  }

  /**
   * Check if value is a string type.
   */
  public boolean isString() {
    return type == ValueType.STRING;
  }

  // ===== Factory Methods =====

  /**
   * Create a string value from bytes.
   * @param bytes the value bytes
   * @return new StoredValue instance
   */
  public static StoredValue bytes(byte[] bytes) {
    return new StoredValue(bytes, ValueType.STRING);
  }

  /**
   * Create a string value from UTF-8 string.
   * @param string the value string
   * @return new StoredValue instance
   */
  public static StoredValue string(String string) {
    if (string == null) {
      throw new IllegalArgumentException("String cannot be null");
    }
    return new StoredValue(string.getBytes(StandardCharsets.UTF_8), ValueType.STRING);
  }

  @Override
  public String toString() {
    return "StoredValue{type=" + type + ", length=" + value.length + "}";
  }
}
