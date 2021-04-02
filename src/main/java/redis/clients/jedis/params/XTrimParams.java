package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.MAXLEN;
import static redis.clients.jedis.Protocol.Keyword.MINID;

public class XTrimParams extends Params {

  private Long maxLen;

  private boolean approximateTrimming;

  private boolean exactTrimming;

  private String minId;

  private Long limit;

  public static XTrimParams xTrimParams() {
    return new XTrimParams();
  }

  public XTrimParams maxLen(long maxLen) {
    this.maxLen = maxLen;
    return this;
  }

  public XTrimParams minId(String minId) {
    this.minId = minId;
    return this;
  }

  public XTrimParams approximateTrimming() {
    this.approximateTrimming = true;
    return this;
  }

  public XTrimParams exactTrimming() {
    this.exactTrimming = true;
    return this;
  }

  public XTrimParams limit(long limit) {
    this.limit = limit;
    return this;
  }

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    List<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);

    if (maxLen != null) {
      byteParams.add(MAXLEN.getRaw());

      if (approximateTrimming) {
        byteParams.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        byteParams.add(Protocol.BYTES_EQUAL);
      }

      byteParams.add(Protocol.toByteArray(maxLen));
    } else if (minId != null) {
      byteParams.add(MINID.getRaw());

      if (approximateTrimming) {
        byteParams.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        byteParams.add(Protocol.BYTES_EQUAL);
      }

      byteParams.add(SafeEncoder.encode(minId));
    }

    if (limit != null) {
      byteParams.add(LIMIT.getRaw());
      byteParams.add(Protocol.toByteArray(limit));
    }

    Collections.addAll(byteParams, args);
    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
