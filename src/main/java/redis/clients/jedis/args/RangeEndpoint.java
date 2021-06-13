package redis.clients.jedis.args;

import redis.clients.jedis.Protocol;

public interface RangeEndpoint<T> extends Rawable {

  RangeEndpoint<T> inclusive();

  RangeEndpoint<T> exclusive();

  public static abstract class AbstractRangeEndpoint<T> implements RangeEndpoint<T> {

    protected static final byte EXCLUSIVE_PREFIX = '(';

    private byte[] raw;

    public AbstractRangeEndpoint(T data) {
      if (data instanceof Rawable) {
        this.raw = ((Rawable) data).getRaw();
      } else if (data instanceof Integer) {
        this.raw = Protocol.toByteArray((Integer) data);
      } else if (data instanceof Double) {
        this.raw = Protocol.toByteArray((Double) data);
      } else {
        throw new IllegalArgumentException(); // TODO message
      }
    }

    @Override
    public RangeEndpoint<T> exclusive() {
      byte[] b = new byte[1 + raw.length];
      b[0] = EXCLUSIVE_PREFIX;
      System.arraycopy(raw, 0, b, 1, raw.length);
      this.raw = b;
      return this;
    }

    @Override
    public RangeEndpoint<T> inclusive() {
      return this;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static RangeEndpoint<Double> of(double value) {
    return new AbstractRangeEndpoint<Double>(value) { };
  }

}
