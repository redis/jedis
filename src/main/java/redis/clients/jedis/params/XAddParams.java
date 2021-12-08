package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.MAXLEN;
import static redis.clients.jedis.Protocol.Keyword.MINID;
import static redis.clients.jedis.Protocol.Keyword.NOMKSTREAM;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class XAddParams implements IParams {

  private String id;

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

  public XAddParams id(String id) {
    this.id = id;
    return this;
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
      args.add(NOMKSTREAM.getRaw());
    }
    if (maxLen != null) {
      args.add(MAXLEN.getRaw());

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(Protocol.toByteArray(maxLen));
    } else if (minId != null) {
      args.add(MINID.getRaw());

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(SafeEncoder.encode(minId));
    }

    if (limit != null) {
      args.add(LIMIT.getRaw());
      args.add(Protocol.toByteArray(limit));
    }

    if (id != null) {
      args.add(SafeEncoder.encode(id));
    } else {
      args.add(Protocol.BYTES_ASTERISK);
    }
  }
}
