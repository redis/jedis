package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.stream.StreamEntryID;
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
      args.addObject(IDLE.getRaw());
      args.addObject(Protocol.toByteArray(idle));
    }

    if (start == null) {
      args.addObject(SafeEncoder.encode("-"));
    } else {
      args.addObject(SafeEncoder.encode(start.toString()));
    }

    if (end == null) {
      args.addObject(SafeEncoder.encode("+"));
    } else {
      args.addObject(SafeEncoder.encode(end.toString()));
    }

    if (count != null) {
      args.addObject(Protocol.toByteArray(count));
    }

    if (consumer != null) {
      args.addObject(SafeEncoder.encode(consumer));
    }
  }
}
