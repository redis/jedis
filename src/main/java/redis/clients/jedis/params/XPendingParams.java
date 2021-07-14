package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.RangeEndpoint;
import redis.clients.jedis.args.StreamEntryIdFactory;
import redis.clients.jedis.util.SafeEncoder;

public class XPendingParams extends Params {

  private Long idle;

  private String consumer;

  private StreamEntryID start;

  private RangeEndpoint<redis.clients.jedis.args.StreamEntryID> startEndpoint
      = RangeEndpoint.of(StreamEntryIdFactory.SMALLEST);

  private StreamEntryID end;

  private RangeEndpoint<redis.clients.jedis.args.StreamEntryID> endEndpoint
      = RangeEndpoint.of(StreamEntryIdFactory.LARGEST);

  private Integer count;

  public static XPendingParams xPendingParams() {
    return new XPendingParams();
  }

  public XPendingParams idle(long idle) {
    this.idle = idle;
    return this;
  }

  /**
   * @deprecated Use {@link #start(redis.clients.jedis.args.RangeEndpoint)}.
   */
  @Deprecated
  public XPendingParams start(StreamEntryID start) {
    this.start = start;
    return this;
  }

  public XPendingParams start(RangeEndpoint<redis.clients.jedis.args.StreamEntryID> start) {
    this.startEndpoint = start;
    return this;
  }

  /**
   * @deprecated Use {@link #end(redis.clients.jedis.args.RangeEndpoint)}.
   */
  @Deprecated
  public XPendingParams end(StreamEntryID end) {
    this.end = end;
    return this;
  }

  public XPendingParams end(RangeEndpoint<redis.clients.jedis.args.StreamEntryID> end) {
    this.endEndpoint = end;
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
  public byte[][] getByteParams() {
    if (start != null && startEndpoint == null) {
      startEndpoint = RangeEndpoint.of(RangeEndpoint.convert(start));
    }
    if (end != null && endEndpoint == null) {
      endEndpoint = RangeEndpoint.of(RangeEndpoint.convert(end));
    }

    List<byte[]> byteParams = new ArrayList<>();

    if (idle != null) {
      byteParams.add(IDLE.getRaw());
      byteParams.add(Protocol.toByteArray(idle));
    }

    if (start == null) {
      byteParams.add(startEndpoint.getRaw());
    } else {
      byteParams.add(SafeEncoder.encode(start.toString()));
    }

    if (end == null) {
      byteParams.add(endEndpoint.getRaw());
    } else {
      byteParams.add(SafeEncoder.encode(end.toString()));
    }
    
    byteParams.add(Protocol.toByteArray(count));

    if (consumer != null) {
      byteParams.add(SafeEncoder.encode(consumer));
    }
    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
