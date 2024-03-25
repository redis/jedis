package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;

import java.util.Objects;

public class XAddParams implements IParams {

  private Rawable id;

  private Long maxLen;

  private boolean approximateTrimming;

  private boolean exactTrimming;

  private boolean nomkstream;

  private String minId;

  private Long limit;

  public static XAddParams xAddParams() {
    return new XAddParams();
  }

  public XAddParams noMkStream() {
    this.nomkstream = true;
    return this;
  }

  public XAddParams id(byte[] id) {
    this.id = RawableFactory.from(id);
    return this;
  }

  public XAddParams id(String id) {
    this.id = RawableFactory.from(id);
    return this;
  }

  public XAddParams id(StreamEntryID id) {
    return id(id.toString());
  }

  public XAddParams id(long time, long sequence) {
    return id(time + "-" + sequence);
  }

  public XAddParams id(long time) {
    return id(time + "-*");
  }

  public XAddParams maxLen(long maxLen) {
    this.maxLen = maxLen;
    return this;
  }

  public XAddParams minId(String minId) {
    this.minId = minId;
    return this;
  }

  public XAddParams approximateTrimming() {
    this.approximateTrimming = true;
    return this;
  }

  public XAddParams exactTrimming() {
    this.exactTrimming = true;
    return this;
  }

  public XAddParams limit(long limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (nomkstream) {
      args.add(Keyword.NOMKSTREAM);
    }

    if (maxLen != null) {
      args.add(Keyword.MAXLEN);

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(maxLen);
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

    args.add(id != null ? id : StreamEntryID.NEW_ENTRY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XAddParams that = (XAddParams) o;
    return approximateTrimming == that.approximateTrimming && exactTrimming == that.exactTrimming && nomkstream == that.nomkstream && Objects.equals(id, that.id) && Objects.equals(maxLen, that.maxLen) && Objects.equals(minId, that.minId) && Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, maxLen, approximateTrimming, exactTrimming, nomkstream, minId, limit);
  }
}
