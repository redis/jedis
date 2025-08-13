package redis.clients.jedis.timeseries;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TimeSeriesProtocol {

  public static final byte[] PLUS = SafeEncoder.encode("+");
  public static final byte[] MINUS = SafeEncoder.encode("-");

  public enum TimeSeriesCommand implements ProtocolCommand {

    CREATE("TS.CREATE", true),
    RANGE("TS.RANGE", false),
    REVRANGE("TS.REVRANGE", false),
    MRANGE("TS.MRANGE", false),
    MREVRANGE("TS.MREVRANGE", false),
    CREATERULE("TS.CREATERULE", true),
    DELETERULE("TS.DELETERULE", true),
    ADD("TS.ADD", true),
    MADD("TS.MADD", true),
    DEL("TS.DEL", true),
    INCRBY("TS.INCRBY", true),
    DECRBY("TS.DECRBY", true),
    INFO("TS.INFO", false),
    GET("TS.GET", false),
    MGET("TS.MGET", false),
    ALTER("TS.ALTER", true),
    QUERYINDEX("TS.QUERYINDEX", false);

    private final byte[] raw;

    private final boolean isWriteCommand;

    private TimeSeriesCommand(String alt, boolean isWriteCommand) {
      raw = SafeEncoder.encode(alt);
      this.isWriteCommand = isWriteCommand;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public boolean isWriteCommand() {
      return isWriteCommand;
    }
  }

  public enum TimeSeriesKeyword implements Rawable {

    RESET,
    FILTER,
    AGGREGATION,
    LABELS,
    RETENTION,
    TIMESTAMP,
    WITHLABELS,
    SELECTED_LABELS,
    COUNT,
    ENCODING,
    COMPRESSED,
    UNCOMPRESSED,
    CHUNK_SIZE,
    DUPLICATE_POLICY,
    IGNORE,
    ON_DUPLICATE,
    ALIGN,
    FILTER_BY_TS,
    FILTER_BY_VALUE,
    GROUPBY,
    REDUCE,
    DEBUG,
    LATEST,
    EMPTY,
    BUCKETTIMESTAMP;

    private final byte[] raw;

    private TimeSeriesKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
