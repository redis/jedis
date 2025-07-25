package redis.clients.jedis.resps;

/**
 * Represents the result of a stream entry deletion operation for XDELEX and XACKDEL commands.
 * - NOT_FOUND (-1): ID doesn't exist in stream
 * - DELETED (1): Entry was deleted/acknowledged and deleted
 * - ACKNOWLEDGED_NOT_DELETED (2): Entry was acknowledged but not deleted (still has dangling references)
 */
public enum StreamEntryDeletionResult {
  
  /**
   * The stream entry ID doesn't exist in the stream.
   * Returned when trying to delete/acknowledge a non-existent entry.
   */
  NOT_FOUND(-1),
  
  /**
   * The entry was successfully deleted/acknowledged and deleted.
   * This is the typical successful case.
   */
  DELETED(1),
  
  /**
   * The entry was acknowledged but not deleted because it still has dangling references
   * in other consumer groups' pending entry lists.
   */
  ACKNOWLEDGED_NOT_DELETED(2);
  
  private final int code;
  
  StreamEntryDeletionResult(int code) {
    this.code = code;
  }
  
  /**
   * Gets the numeric code returned by Redis for this result.
   * 
   * @return the numeric code (-1, 1, or 2)
   */
  public int getCode() {
    return code;
  }
  
  /**
   * Creates a StreamEntryDeletionResult from the numeric code returned by Redis.
   * 
   * @param code the numeric code from Redis
   * @return the corresponding StreamEntryDeletionResult
   * @throws IllegalArgumentException if the code is not recognized
   */
  public static StreamEntryDeletionResult fromCode(int code) {
    switch (code) {
      case -1:
        return NOT_FOUND;
      case 1:
        return DELETED;
      case 2:
        return ACKNOWLEDGED_NOT_DELETED;
      default:
        throw new IllegalArgumentException("Unknown stream entry deletion result code: " + code);
    }
  }
  
  /**
   * Creates a StreamEntryDeletionResult from a Long value returned by Redis.
   * 
   * @param value the Long value from Redis
   * @return the corresponding StreamEntryDeletionResult
   * @throws IllegalArgumentException if the value is null or not recognized
   */
  public static StreamEntryDeletionResult fromLong(Long value) {
    if (value == null) {
      throw new IllegalArgumentException("Stream entry deletion result value cannot be null");
    }
    return fromCode(value.intValue());
  }
  
  @Override
  public String toString() {
    return name() + "(" + code + ")";
  }
}
