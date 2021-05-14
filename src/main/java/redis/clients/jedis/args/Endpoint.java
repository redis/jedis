package redis.clients.jedis.args;

import redis.clients.jedis.Protocol;

public interface Endpoint<T> extends Rawable {

  Endpoint<T> inclusive();

  Endpoint<T> exclusive();

  public static abstract class AbstractEndpoint<T> implements Endpoint<T> {

    private final byte[] raw;
    private boolean exclusive;

    public AbstractEndpoint(T data, boolean exclusive) {
      if (data instanceof Rawable) {
        this.raw = ((Rawable) data).getRaw();
      } else if (data instanceof Integer) {
        this.raw = Protocol.toByteArray((Integer) data);
      } else if (data instanceof Double) {
        this.raw = Protocol.toByteArray((Double) data);
      } else {
        throw new IllegalArgumentException(); // TODO message
      }
      this.exclusive = exclusive;
    }

    public AbstractEndpoint(T data) {
      this(data, false);
    }

    @Override
    public Endpoint<T> exclusive() {
      exclusive = true;
      return this;
    }

    @Override
    public Endpoint<T> inclusive() {
      exclusive = false;
      return this;
    }

    @Override
    public byte[] getRaw() {
      if (!exclusive) {
        return raw;
      }
      byte[] b = new byte[1 + raw.length];
      b[0] = EXCLUSIVE_PREFIX;
      System.arraycopy(raw, 0, b, 1, raw.length);
      return b;
    }
  }

  static final byte EXCLUSIVE_PREFIX = '(';

  public static Endpoint<Double> of(double value) {
    return new AbstractEndpoint<Double>(value) {
    };
  }

  public static Endpoint<Double> of(double value, boolean exclusive) {
    return new AbstractEndpoint<Double>(value, exclusive) {
    };
  }

  public static Endpoint<StreamEntryID> of(StreamEntryID value) {
    return new AbstractEndpoint<StreamEntryID>(value) {
    };
  }

  public static Endpoint<StreamEntryID> of(StreamEntryID value, boolean exclusive) {
    return new AbstractEndpoint<StreamEntryID>(value, exclusive) {
    };
  }

  // Just to help out
  public static StreamEntryID convert(redis.clients.jedis.StreamEntryID id) {
    String str = id.toString();
    if (str.length() == 1) {
      return new StreamEntryIdFactory.SpecialStreamEntryID(str);
    }
//    if (id.getSequence() == 0) {
//      return new StreamEntryIdFactory.DefaultStreamEntryID(id.getTime());
//    }
    return new StreamEntryIdFactory.DefaultStreamEntryID(id.getTime(), id.getSequence());
  }
}
