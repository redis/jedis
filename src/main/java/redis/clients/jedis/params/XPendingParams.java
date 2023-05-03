package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;
import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.args.RawableFactory.from;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.Rawable;

public class XPendingParams implements IParams {

  private Long idle;
  private Rawable start;
  private Rawable end;
  private Integer count;
  private Rawable consumer;

  public XPendingParams(StreamEntryID start, StreamEntryID end, int count) {
    this(start.toString(), end.toString(), count);
  }

  public XPendingParams(String start, String end, int count) {
    this(from(start), from(end), count);
  }

  public XPendingParams(byte[] start, byte[] end, int count) {
    this(from(start), from(end), count);
  }

  private XPendingParams(Rawable start, Rawable end, Integer count) {
    this.start = start;
    this.end = end;
    this.count = count;
  }

  public XPendingParams() {
    this.start = null;
    this.end = null;
    this.count = null;
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

  public static XPendingParams xPendingParams() {
    return new XPendingParams();
  }

  public XPendingParams idle(long idle) {
    this.idle = idle;
    return this;
  }

  public XPendingParams start(StreamEntryID start) {
    if (this.start != null) throw new IllegalStateException("'start' is already set.");
    this.start = from(start.toString());
    return this;
  }

  public XPendingParams end(StreamEntryID end) {
    if (this.end != null) throw new IllegalStateException("'end' is already set.");
    this.end = from(end.toString());
    return this;
  }

  public XPendingParams count(int count) {
    if (this.count != null) throw new IllegalStateException("'count' is already set.");
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
    if (count == null) {
      throw new IllegalStateException("start, end and count must be set.");
    }
    if (start == null) start = from("-");
    if (end == null) end = from("+");

    if (idle != null) {
      args.add(IDLE).add(toByteArray(idle));
    }

    args.add(start).add(end).add(toByteArray(count));

    if (consumer != null) {
      args.add(consumer);
    }
  }
}
