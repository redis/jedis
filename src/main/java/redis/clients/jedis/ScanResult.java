package redis.clients.jedis;

import java.util.List;

import redis.clients.util.SafeEncoder;

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

  public String getCursor() {
    return SafeEncoder.encode(cursor);
  }

  public byte[] getCursorAsBytes() {
    return cursor;
  }

  public List<T> getResult() {
    return results;
  }
}
