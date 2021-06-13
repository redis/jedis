package redis.clients.jedis.args;

import redis.clients.jedis.Protocol;

public interface Endpoint<T> extends Rawable {

  Endpoint<T> inclusive();

  Endpoint<T> exclusive();

  public static abstract class AbstractEndpoint<T> implements Endpoint<T> {

    private byte[] raw;

    public AbstractEndpoint(T data) {
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
    public Endpoint<T> exclusive() {
      byte[] b = new byte[1 + raw.length];
      b[0] = EXCLUSIVE_PREFIX;
      System.arraycopy(raw, 0, b, 1, raw.length);
      this.raw = b;
      return this;
    }

    @Override
    public Endpoint<T> inclusive() {
      return this;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  static final byte EXCLUSIVE_PREFIX = '(';

  public static Endpoint<Double> of(double value) {
    return new AbstractEndpoint<Double>(value) {
    };
  }

}
