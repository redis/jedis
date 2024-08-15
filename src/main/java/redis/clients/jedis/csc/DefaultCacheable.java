package redis.clients.jedis.csc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

public class DefaultCacheable implements Cacheable {

  public static final DefaultCacheable INSTANCE = new DefaultCacheable();

  private static final Set<ProtocolCommand> DEFAULT_CACHEABLE_COMMANDS = new HashSet<ProtocolCommand>() {
    {
      add(Command.BITCOUNT);
      add(Command.BITFIELD_RO);
      add(Command.BITPOS);
      add(Command.EXISTS);
      add(Command.GEODIST);
      add(Command.GEOHASH);
      add(Command.GEOPOS);
      add(Command.GEORADIUSBYMEMBER_RO);
      add(Command.GEORADIUS_RO);
      add(Command.GEOSEARCH);
      add(Command.GET);
      add(Command.GETBIT);
      add(Command.GETRANGE);
      add(Command.HEXISTS);
      add(Command.HGET);
      add(Command.HGETALL);
      add(Command.HKEYS);
      add(Command.HLEN);
      add(Command.HMGET);
      add(Command.HSTRLEN);
      add(Command.HVALS);
      add(JsonCommand.ARRINDEX);
      add(JsonCommand.ARRLEN);
      add(JsonCommand.GET);
      add(JsonCommand.MGET);
      add(JsonCommand.OBJKEYS);
      add(JsonCommand.OBJLEN);
      add(JsonCommand.STRLEN);
      add(JsonCommand.TYPE);
      add(Command.LCS);
      add(Command.LINDEX);
      add(Command.LLEN);
      add(Command.LPOS);
      add(Command.LRANGE);
      add(Command.MGET);
      add(Command.SCARD);
      add(Command.SDIFF);
      add(Command.SINTER);
      add(Command.SISMEMBER);
      add(Command.SMEMBERS);
      add(Command.SMISMEMBER);
      add(Command.STRLEN);
      add(Command.SUBSTR);
      add(Command.SUNION);
      add(TimeSeriesCommand.GET);
      add(TimeSeriesCommand.INFO);
      add(TimeSeriesCommand.RANGE);
      add(TimeSeriesCommand.REVRANGE);
      add(Command.TYPE);
      add(Command.XLEN);
      add(Command.XPENDING);
      add(Command.XRANGE);
      add(Command.XREVRANGE);
      add(Command.ZCARD);
      add(Command.ZCOUNT);
      add(Command.ZLEXCOUNT);
      add(Command.ZMSCORE);
      add(Command.ZRANGE);
      add(Command.ZRANGEBYLEX);
      add(Command.ZRANGEBYSCORE);
      add(Command.ZRANK);
      add(Command.ZREVRANGE);
      add(Command.ZREVRANGEBYLEX);
      add(Command.ZREVRANGEBYSCORE);
      add(Command.ZREVRANK);
      add(Command.ZSCORE);
    }
  };

  public DefaultCacheable() {
  }

  public static boolean isDefaultCacheableCommand(ProtocolCommand command) {
    return DEFAULT_CACHEABLE_COMMANDS.contains(command);
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    return isDefaultCacheableCommand(command);
  }
}
