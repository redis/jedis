package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;
import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.util.SafeEncoder.encode;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.StreamEntryID;

public class XPendingParams implements IParams {

  private boolean legacy = true;
  private Long idle;
  private byte[] start; // TODO: final
  private byte[] end; // TODO: final
  private Integer count; // TODO: final
  private String consumer;

  /**
   * @deprecated Use {@link XPendingParams#XPendingParams(redis.clients.jedis.StreamEntryID, redis.clients.jedis.StreamEntryID, int)}.
   */
  @Deprecated
  public XPendingParams() {
  }

  /**
   * @deprecated Use {@link XPendingParams#xPendingParams(redis.clients.jedis.StreamEntryID, redis.clients.jedis.StreamEntryID, int)}.
   */
  @Deprecated
  public static XPendingParams xPendingParams() {
    return new XPendingParams();
  }

  public XPendingParams(StreamEntryID start, StreamEntryID end, int count) {
    this(start.toString(), end.toString(), count);
  }

  public XPendingParams(String start, String end, int count) {
    this(encode(start), encode(end), count);
  }

  public XPendingParams(byte[] start, byte[] end, int count) {
    this.legacy = false;
    this.start = start;
    this.end = end;
    this.count = count;
  }

  public static XPendingParams xPendingParams(StreamEntryID start, StreamEntryID end, int count) {
    return new XPendingParams(start, end, count);
  }

  public static XPendingParams xPendingParams(String start, String end, int count) {
    return new XPendingParams(start, end, count);
  }

  public static XPendingParams xPendingParams(byte[] start, byte[] end, int count) {
    return new XPendingParams(start, end, count);
  }

  public XPendingParams idle(long idle) {
    this.idle = idle;
    return this;
  }

  @Deprecated
  public XPendingParams start(StreamEntryID start) {
    this.start = encode(start.toString());
    return this;
  }

  @Deprecated
  public XPendingParams end(StreamEntryID end) {
    this.end = encode(end.toString());
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
      args.add(IDLE).add(toByteArray(idle));
    }

    if (legacy) {
      if (start == null) {
        args.add(encode("-"));
      } else {
        args.add(start);
      }

      if (end == null) {
        args.add(encode("+"));
      } else {
        args.add(end);
      }

      if (count != null) {
        args.add(toByteArray(count));
      }
    } else {
      args.add(start).add(end).add(toByteArray(count));
    }

    if (consumer != null) {
      args.add(consumer);
    }
  }
}
