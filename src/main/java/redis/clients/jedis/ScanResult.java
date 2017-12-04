package redis.clients.jedis;

import java.util.List;

import redis.clients.jedis.util.SafeEncoder;

public class ScanResult<T> {
  private byte[] cursor;
  private List<T> results;

  public ScanResult(String cursor, List<T> results) {
    this(SafeEncoder.encode(cursor), results);
  }

  public ScanResult(byte[] cursor, List<T> results) {
    this.cursor = cursor;
    this.results = results;
  }

  /**
   * Returns the new value of the cursor
   * @return the new cursor value. {@link ScanParams#SCAN_POINTER_START} when a complete iteration has finished
   */
  public String getCursor() {
    return SafeEncoder.encode(cursor);
  }

  /**
   * Is the iteration complete. I.e. was the complete dataset scanned.
   *
   * @return true if the iteration is complete
   */
  public boolean isCompleteIteration() {
    return ScanParams.SCAN_POINTER_START.equals(getCursor());
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
