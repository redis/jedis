package redis.clients.jedis.gears;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class RedisGearsProtocol {
  public enum GearsCommand implements ProtocolCommand {
    TFUNCTION("TFUNCTION"),
    TFCALL("TFCALL"),
    TFCALLASYNC("TFCALLASYNC");

    private final byte[] raw;

    GearsCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public enum GearsKeyword {
    CONFIG("CONFIG"),
    REPLACE("REPLACE"),
    LOAD("LOAD"),
    DELETE("DELETE"),
    LIST("LIST"),
    WITHCODE("WITHCODE"),
    LIBRARY("LIBRARY"),
    VERBOSE("VERBOSE");

    private final String value;

    GearsKeyword(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
