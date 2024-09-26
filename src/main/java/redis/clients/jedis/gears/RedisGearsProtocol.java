package redis.clients.jedis.gears;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

@Deprecated
public class RedisGearsProtocol {

  @Deprecated
  public enum GearsCommand implements ProtocolCommand {

    @Deprecated TFUNCTION,
    @Deprecated TFCALL,
    @Deprecated TFCALLASYNC;

    private final byte[] raw;

    private GearsCommand() {
      this.raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  @Deprecated
  public enum GearsKeyword implements Rawable {

    CONFIG,
    REPLACE,
    LOAD,
    DELETE,
    LIST,
    WITHCODE,
    LIBRARY,
    VERBOSE;

    private final byte[] raw;

    private GearsKeyword() {
      this.raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
