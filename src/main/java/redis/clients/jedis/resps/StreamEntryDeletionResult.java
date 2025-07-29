package redis.clients.jedis.resps;

/**
 * Represents the result of a stream entry deletion operation for XDELEX and XACKDEL commands.
 * <ul>
 * <li>NOT_FOUND (-1): ID doesn't exist in stream</li>
 * <li>DELETED (1): Entry was deleted/acknowledged and deleted</li>
 * <li>NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED (2): Entry wasn't deleted.</li>
 * </ul>
 */
public enum StreamEntryDeletionResult {

  /**
   * The stream entry ID doesn't exist in the stream.
   * <p>
   * Returned when trying to delete/acknowledge a non-existent entry.
   * </p>
   */
  NOT_FOUND(-1),

  /**
   * The entry was successfully deleted/acknowledged and deleted.
   * <p>
   * This is the typical successful case.
   * </p>
   */
  DELETED(1),

  /**
   * The entry was not deleted due to one of the following reasons:
   * <ul>
   * <li>For XDELEX: The entry was not acknowledged by any consumer group</li>
   * <li>For XACKDEL: The entry still has pending references in other consumer groups</li>
   * </ul>
   */
  NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED(2);

  private final int code;

  StreamEntryDeletionResult(int code) {
    this.code = code;
  }

  /**
   * Gets the numeric code returned by Redis for this result.
   * @return the numeric code (-1, 1, or 2)
   */
  public int getCode() {
    return code;
  }

  /**
   * Creates a StreamEntryDeletionResult from the numeric code returned by Redis.
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
        return NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED;
      default:
        throw new IllegalArgumentException("Unknown stream entry deletion result code: " + code);
    }
  }

  /**
   * Creates a StreamEntryDeletionResult from a Long value returned by Redis.
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
