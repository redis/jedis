package redis.clients.jedis.tairhash;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TairHashProtocol {

  public enum TairHashCommand implements ProtocolCommand {
    EXHSET("EXHSET"), EXHMSET("EXHMSET"), EXHPEXPIREAT("EXHPEXPIREAT"), EXHPEXPIRE("EXHPEXPIRE"),
    EXHEXPIREAT("EXHEXPIREAT"), EXHEXPIRE("EXHEXPIRE"), EXHPTTL("EXHPTTL"), EXHTTL("EXHTTL"),
    EXHVER("EXHVER"), EXHSETVER("EXHSETVER"), EXHINCRBY("EXHINCRBY"), EXHINCRBYFLOAT(
        "EXHINCRBYFLOAT"), EXHGET("EXHGET"), EXHGETWITHVER("EXHGETWITHVER"), EXHMGET("EXHMGET"),
    EXHDEL("EXHDEL"), EXHLEN("EXHLEN"), EXHEXISTS("EXHEXISTS"), EXHSTRLEN("EXHSTRLEN"), EXHKEYS(
        "EXHKEYS"), EXHVALS("EXHVALS"), EXHGETALL("EXHGETALL"), EXHMGETWITHVER("EXHMGETWITHVER"),
    EXHSCAN("EXHSCAN");

    private final byte[] raw;

    private TairHashCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
