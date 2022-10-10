package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;
import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.args.RawableFactory.from;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.Rawable;

public class XPendingParams implements IParams {

  private boolean legacy = true;
  private Long idle;
  private Rawable start; // TODO: final
  private Rawable end; // TODO: final
  private int count = Integer.MIN_VALUE; // TODO: final
  private Rawable consumer;

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
    this(from(start), from(end), count);
  }

  public XPendingParams(byte[] start, byte[] end, int count) {
    this(from(start), from(end), count);
  }

  private XPendingParams(Rawable start, Rawable end, int count) {
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
    this.start = from(start.toString());
    return this;
  }

  @Deprecated
  public XPendingParams end(StreamEntryID end) {
    this.end = from(end.toString());
    return this;
  }

  public XPendingParams count(int count) {
    this.count = count;
    return this;
  }

  public XPendingParams consumer(String consumer) {
    this.consumer = from(consumer);
    return this;
  }

  public XPendingParams consumer(byte[] consumer) {
    this.consumer = from(consumer);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (idle != null) {
      args.add(IDLE).add(toByteArray(idle));
    }

    if (legacy) {
      if (start == null) {
        args.add("-");
      } else {
        args.add(start);
      }

      if (end == null) {
        args.add("+");
      } else {
        args.add(end);
      }

      if (count != Integer.MIN_VALUE) {
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
