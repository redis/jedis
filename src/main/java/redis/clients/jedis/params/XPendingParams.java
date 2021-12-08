package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.util.SafeEncoder;

public class XPendingParams implements IParams {

  private Long idle;

  private String consumer;

  private StreamEntryID start;

  private StreamEntryID end;

  private Integer count;

  public static XPendingParams xPendingParams() {
    return new XPendingParams();
  }

  public XPendingParams idle(long idle) {
    this.idle = idle;
    return this;
  }

  public XPendingParams start(StreamEntryID start) {
    this.start = start;
    return this;
  }

  public XPendingParams end(StreamEntryID end) {
    this.end = end;
    return this;
  }

  public XPendingParams count(int count) {
    this.count = count;
    return this;
  }

  public XPendingParams consumer(String consumer) {
    this.consumer = consumer;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (idle != null) {
      args.add(IDLE.getRaw());
      args.add(Protocol.toByteArray(idle));
    }

    if (start == null) {
      args.add(SafeEncoder.encode("-"));
    } else {
      args.add(SafeEncoder.encode(start.toString()));
    }

    if (end == null) {
      args.add(SafeEncoder.encode("+"));
    } else {
      args.add(SafeEncoder.encode(end.toString()));
    }

    if (count != null) {
      args.add(Protocol.toByteArray(count));
    }

    if (consumer != null) {
      args.add(SafeEncoder.encode(consumer));
    }
  }
}
