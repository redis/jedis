package redis.clients.jedis.bloom;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class RedisBloomProtocol {

  public enum BloomFilterCommand implements ProtocolCommand {

    RESERVE("BF.RESERVE", true),
    ADD("BF.ADD", true),
    MADD("BF.MADD", true),
    EXISTS("BF.EXISTS", false),
    MEXISTS("BF.MEXISTS", false),
    INSERT("BF.INSERT", true),
    SCANDUMP("BF.SCANDUMP", true),
    LOADCHUNK("BF.LOADCHUNK", true),
    CARD("BF.CARD", false),
    INFO("BF.INFO", false);

    private final byte[] raw;
    private final boolean isWriteCommand;

    private BloomFilterCommand(String alt, boolean isWriteCommand) {
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

  public enum CuckooFilterCommand implements ProtocolCommand {

    RESERVE("CF.RESERVE", true), //
    ADD("CF.ADD", true), //
    ADDNX("CF.ADDNX", true), //
    INSERT("CF.INSERT", true), //
    INSERTNX("CF.INSERTNX", true), //
    EXISTS("CF.EXISTS", false), //
    MEXISTS("CF.MEXISTS", false), //
    DEL("CF.DEL", true), //
    COUNT("CF.COUNT", false), //
    SCANDUMP("CF.SCANDUMP", true), //
    LOADCHUNK("CF.LOADCHUNK", true), //
    INFO("CF.INFO", false);

    private final byte[] raw;
    private final boolean isWriteCommand;

    private CuckooFilterCommand(String alt, boolean isWriteCommand) {
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

  public enum CountMinSketchCommand implements ProtocolCommand {

    INITBYDIM("CMS.INITBYDIM", true), //
    INITBYPROB("CMS.INITBYPROB", true), //
    INCRBY("CMS.INCRBY", true), //
    QUERY("CMS.QUERY", false), //
    MERGE("CMS.MERGE", true), //
    INFO("CMS.INFO", false);

    private final byte[] raw;
    private final boolean isWriteCommand;

    private CountMinSketchCommand(String alt, boolean isWriteCommand) {
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

  public enum TopKCommand implements ProtocolCommand {

    RESERVE("TOPK.RESERVE", true),
    ADD("TOPK.ADD", true),
    INCRBY("TOPK.INCRBY", true),
    QUERY("TOPK.QUERY", false),
    LIST("TOPK.LIST", false),
    INFO("TOPK.INFO", false);

    private final byte[] raw;
    private final boolean isWriteCommand;

    private TopKCommand(String alt, boolean isWriteCommand) {
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

  public enum TDigestCommand implements ProtocolCommand {

    CREATE(true), INFO(false), ADD(true), RESET(true), MERGE(true), CDF(false),
    QUANTILE(false), MIN(false), MAX(false), TRIMMED_MEAN(false),
    RANK(false), REVRANK(false), BYRANK(false), BYREVRANK(false);

    private final byte[] raw;

    private final boolean isWriteCommand;

    private TDigestCommand(boolean isWriteCommand) {
      raw = SafeEncoder.encode("TDIGEST." + name());
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

  public enum RedisBloomKeyword implements Rawable {

    CAPACITY, ERROR, NOCREATE, EXPANSION, NONSCALING, BUCKETSIZE, MAXITERATIONS, ITEMS, WEIGHTS,
    COMPRESSION, OVERRIDE, WITHCOUNT;

    private final byte[] raw;

    private RedisBloomKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
