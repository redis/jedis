package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public final class StreamEntryIdFactory {

  public static StreamEntryID of(long milliseconds, long sequence) {
    return new DefaultStreamEntryID(milliseconds, sequence);
  }

  public static StreamEntryID of(long milliseconds) {
    return new DefaultStreamEntryID(milliseconds);
  }

  /**
   * Should be used only with XADD
   *
   * {@code XADD mystream * field1 value1}
   */
  public static final StreamEntryID NEW = new SpecialStreamEntryID("*");

  /**
   * Should be used only with XGROUP CREATE
   *
   * {@code XGROUP CREATE mystream consumer-group-name $}
   */
  public static final StreamEntryID LAST = new SpecialStreamEntryID("$");

  /**
   * Should be used only with XREADGROUP
   *
   * {@code XREADGROUP $GroupName $ConsumerName BLOCK 2000 COUNT 10 STREAMS mystream >}
   */
  public static final StreamEntryID UNRECEIVED = new SpecialStreamEntryID(">");

  public static final StreamEntryID SMALLEST = new SpecialStreamEntryID("-");

  public static final StreamEntryID LARGEST = new SpecialStreamEntryID("+");

  public static class DefaultStreamEntryID implements StreamEntryID {

    private final long milliseconds;
    private final long sequence;

    public DefaultStreamEntryID(long milliseconds, long sequence) {
      if (milliseconds < 0) {
        throw new IllegalArgumentException("Milliseconds cannot be negative.");
      }
      this.milliseconds = milliseconds;
      this.sequence = sequence;
    }

    public DefaultStreamEntryID(long milliseconds) {
      this(milliseconds, -1);
    }

    @Override
    public long milliseconds() {
      return milliseconds;
    }

    @Override
    public long sequence() {
      return sequence;
    }

    @Override
    public byte[] getRaw() {
      return SafeEncoder.encode(sequence < 0 ? String.format("%d", milliseconds)
          : String.format("%d-%d", milliseconds, sequence));
    }
  }

  public static class SpecialStreamEntryID implements StreamEntryID {

    private final String str;
    private final byte[] raw;

    public SpecialStreamEntryID(String string) {
      this.str = string;
      this.raw = null;
    }

    public SpecialStreamEntryID(byte[] raw) {
      this.str = null;
      this.raw = raw;
    }

    @Override
    public long milliseconds() {
      return -1;
    }

    @Override
    public long sequence() {
      return -1;
    }

    @Override
    public byte[] getRaw() {
      return raw != null ? raw : SafeEncoder.encode(str);
    }

    @Override
    public String toString() {
      return str != null ? str : SafeEncoder.encode(raw);
    }
  }

  private StreamEntryIdFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
