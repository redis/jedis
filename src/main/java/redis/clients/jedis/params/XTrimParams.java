package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class XTrimParams implements IParams {

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

  @Override
  public void addParams(CommandArguments args) {
    if (maxLen != null) {
      args.add(Keyword.MAXLEN);

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(Protocol.toByteArray(maxLen));
    } else if (minId != null) {
      args.add(Keyword.MINID);

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(minId);
    }

    if (limit != null) {
      args.add(Keyword.LIMIT).add(limit);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XTrimParams that = (XTrimParams) o;
    return approximateTrimming == that.approximateTrimming && exactTrimming == that.exactTrimming && Objects.equals(maxLen, that.maxLen) && Objects.equals(minId, that.minId) && Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxLen, approximateTrimming, exactTrimming, minId, limit);
  }
}
