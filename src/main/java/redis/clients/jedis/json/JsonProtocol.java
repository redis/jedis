package redis.clients.jedis.json;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class JsonProtocol {

  public enum JsonCommand implements ProtocolCommand {
    DEL("JSON.DEL", true),
    GET("JSON.GET", false),
    MGET("JSON.MGET", false),
    MERGE("JSON.MERGE", true),
    SET("JSON.SET", true),
    TYPE("JSON.TYPE", false),
    STRAPPEND("JSON.STRAPPEND", true),
    STRLEN("JSON.STRLEN", false),
    NUMINCRBY("JSON.NUMINCRBY", true),
    ARRAPPEND("JSON.ARRAPPEND", true),
    ARRINDEX("JSON.ARRINDEX", false),
    ARRINSERT("JSON.ARRINSERT", true),
    ARRLEN("JSON.ARRLEN", false),
    ARRPOP("JSON.ARRPOP", true),
    ARRTRIM("JSON.ARRTRIM", true),
    CLEAR("JSON.CLEAR", true),
    TOGGLE("JSON.TOGGLE", true),
    OBJKEYS("JSON.OBJKEYS", false),
    OBJLEN("JSON.OBJLEN", false),
    DEBUG("JSON.DEBUG", false),
    RESP("JSON.RESP", false);

    private final byte[] raw;

    private final boolean isWriteCommand;

    private JsonCommand(String alt, boolean isWriteCommand) {
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
}
