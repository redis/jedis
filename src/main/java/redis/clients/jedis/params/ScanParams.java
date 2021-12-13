package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.MATCH;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.Protocol;

import redis.clients.jedis.util.SafeEncoder;

public class ScanParams implements IParams {

  private final Map<Keyword, ByteBuffer> params = new EnumMap<>(Keyword.class);

  public static final String SCAN_POINTER_START = String.valueOf(0);
  public static final byte[] SCAN_POINTER_START_BINARY = SafeEncoder.encode(SCAN_POINTER_START);

  public ScanParams match(final byte[] pattern) {
    params.put(MATCH, ByteBuffer.wrap(pattern));
    return this;
  }

  /**
   * @see <a href="https://redis.io/commands/scan#the-match-option">MATCH option in Redis documentation</a>
   */
  public ScanParams match(final String pattern) {
    params.put(MATCH, ByteBuffer.wrap(SafeEncoder.encode(pattern)));
    return this;
  }

  /**
   * @see <a href="https://redis.io/commands/scan#the-count-option">COUNT option in Redis documentation</a>
   */
  public ScanParams count(final Integer count) {
    params.put(COUNT, ByteBuffer.wrap(Protocol.toByteArray(count)));
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    for (Map.Entry<Keyword, ByteBuffer> param : params.entrySet()) {
      args.add(param.getKey());
      args.add(param.getValue().array());
    }
  }

  public byte[] binaryMatch() {
    if (params.containsKey(MATCH)) {
      return params.get(MATCH).array();
    } else {
      return null;
    }
  }

  public String match() {
    if (params.containsKey(MATCH)) {
      return new String(params.get(MATCH).array());
    } else {
      return null;
    }
  }
}
