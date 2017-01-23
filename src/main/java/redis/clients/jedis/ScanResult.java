package redis.clients.jedis;

import java.util.List;

import redis.clients.util.SafeEncoder;

public class ScanResult<T> {
  private byte[] cursor;
  private List<T> results;

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public ScanResult(int cursor, List<T> results) {
    this(Protocol.toByteArray(cursor), results);
  }

  public ScanResult(String cursor, List<T> results) {
    this(SafeEncoder.encode(cursor), results);
  }

  public ScanResult(byte[] cursor, List<T> results) {
    this.cursor = cursor;
    this.results = results;
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531
   * @return int(currently), but will be changed to String, so be careful to prepare! 
   */
  public int getCursor() {
    return Integer.parseInt(getStringCursor());
  }

  /**
   * Returns the new value of the cursor
   * @return the new cursor value. {@link ScanParams#SCAN_POINTER_START} when a complete iteration has finished
   * FIXME: This method should be changed to getCursor() on next major release
   */
  public String getStringCursor() {
    return SafeEncoder.encode(cursor);
  }

  /**
   * Is the iteration complete. I.e. was the complete dataset scanned.
   *
   * @return true if the iteration is complete
   */
  public boolean isCompleteIteration() {
    return ScanParams.SCAN_POINTER_START.equals(getStringCursor());
  }

  public byte[] getCursorAsBytes() {
    return cursor;
  }

  /**
   * The scan results from the current call.
   * @return the scan results
   */
  public List<T> getResult() {
    return results;
  }
}
