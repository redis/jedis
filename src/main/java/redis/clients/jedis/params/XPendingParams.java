package redis.clients.jedis.params;

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

  private RangeEndpoint<redis.clients.jedis.args.StreamEntryID> startEndpoint
      = RangeEndpoint.of(StreamEntryIdFactory.SMALLEST);

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

  public XPendingParams start(StreamEntryID start) {
    return start(RangeEndpoint.of(RangeEndpoint.convert(start)));
  }

  public XPendingParams start(RangeEndpoint<redis.clients.jedis.args.StreamEntryID> start) {
    this.startEndpoint = start;
    return this;
  }

  public XPendingParams end(StreamEntryID end) {
    return end(RangeEndpoint.of(RangeEndpoint.convert(end)));
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
    List<byte[]> byteParams = new ArrayList<>();

    if (idle != null) {
      byteParams.add(Protocol.Keyword.IDLE.getRaw());
      byteParams.add(Protocol.toByteArray(idle));
    }

    byteParams.add(startEndpoint.getRaw());

    byteParams.add(endEndpoint.getRaw());

    byteParams.add(Protocol.toByteArray(count));

    if (consumer != null) {
      byteParams.add(SafeEncoder.encode(consumer));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
