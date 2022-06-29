package redis.clients.jedis.graph;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class GraphProtocol {

  public enum GraphCommand implements ProtocolCommand {

    QUERY,
    RO_QUERY,
    DELETE,
    LIST,
    PROFILE,
    EXPLAIN,
    SLOWLOG,
    CONFIG;

    private final byte[] raw;

    private GraphCommand() {
      raw = SafeEncoder.encode("GRAPH." + name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public enum GraphKeyword implements Rawable {

    CYPHER,
    TIMEOUT,
    SET,
    GET,
    __COMPACT("--COMPACT");

    private final byte[] raw;

    private GraphKeyword() {
      raw = SafeEncoder.encode(name());
    }

    private GraphKeyword(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
