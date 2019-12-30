package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.MATCH;

import redis.clients.jedis.Protocol.Keyword;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.util.SafeEncoder;

public class ScanParams {

  private final Map<Keyword, ByteBuffer> params = new EnumMap<Keyword, ByteBuffer>(Keyword.class);

  public static final String SCAN_POINTER_START = String.valueOf(0);
  public static final byte[] SCAN_POINTER_START_BINARY = SafeEncoder.encode(SCAN_POINTER_START);

  public ScanParams match(final byte[] pattern) {
    params.put(MATCH, ByteBuffer.wrap(pattern));
    return this;
  }

  /**
   * @see <a href="https://redis.io/commands/scan#the-match-option">MATCH option in Redis documentation</a>
   * 
   * @param pattern
   * @return 
   */
  public ScanParams match(final String pattern) {
    params.put(MATCH, ByteBuffer.wrap(SafeEncoder.encode(pattern)));
    return this;
  }

  /**
   * @see <a href="https://redis.io/commands/scan#the-count-option">COUNT option in Redis documentation</a>
   * 
   * @param count
   * @return 
   */
  public ScanParams count(final Integer count) {
    params.put(COUNT, ByteBuffer.wrap(Protocol.toByteArray(count)));
    return this;
  }

  public Collection<byte[]> getParams() {
    List<byte[]> paramsList = new ArrayList<byte[]>(params.size());
    for (Map.Entry<Keyword, ByteBuffer> param : params.entrySet()) {
      paramsList.add(param.getKey().raw);
      paramsList.add(param.getValue().array());
    }
    return Collections.unmodifiableCollection(paramsList);
  }

  byte[] binaryMatch() {
    if (params.containsKey(MATCH)) {
      return params.get(MATCH).array();
    } else {
      return null;
    }
  }

  String match() {
    if (params.containsKey(MATCH)) {
      return new String(params.get(MATCH).array());
    } else {
      return null;
    }
  }

  Integer count() {
    if (params.containsKey(COUNT)) {
      return params.get(COUNT).getInt();
    } else {
      return null;
    }
  }
}
