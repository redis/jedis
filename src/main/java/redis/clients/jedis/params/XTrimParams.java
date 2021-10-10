package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import redis.clients.jedis.CommandArguments;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.MAXLEN;
import static redis.clients.jedis.Protocol.Keyword.MINID;

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
      args.addObject(MAXLEN.getRaw());

      if (approximateTrimming) {
        args.addObject(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.addObject(Protocol.BYTES_EQUAL);
      }

      args.addObject(Protocol.toByteArray(maxLen));
    } else if (minId != null) {
      args.addObject(MINID.getRaw());

      if (approximateTrimming) {
        args.addObject(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.addObject(Protocol.BYTES_EQUAL);
      }

      args.addObject(SafeEncoder.encode(minId));
    }

    if (limit != null) {
      args.addObject(LIMIT.getRaw());
      args.addObject(Protocol.toByteArray(limit));
    }
  }
}
