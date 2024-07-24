package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

public class DefaultClientSideCacheable implements ClientSideCacheable {

  public static final DefaultClientSideCacheable INSTANCE = new DefaultClientSideCacheable();

  private Map<ProtocolCommand, Boolean> commandsToCache = new HashMap<ProtocolCommand, Boolean>() {
    {
      put(Command.BITCOUNT, true);
      put(Command.BITFIELD_RO, true);
      put(Command.BITPOS, true);
      put(Command.EXISTS, true);
      put(Command.GEODIST, true);
      put(Command.GEOHASH, true);
      put(Command.GEOPOS, true);
      put(Command.GEORADIUSBYMEMBER_RO, true);
      put(Command.GEORADIUS_RO, true);
      put(Command.GEOSEARCH, true);
      put(Command.GET, true);
      put(Command.GETBIT, true);
      put(Command.GETRANGE, true);
      put(Command.HEXISTS, true);
      put(Command.HGET, true);
      put(Command.HGETALL, true);
      put(Command.HKEYS, true);
      put(Command.HLEN, true);
      put(Command.HMGET, true);
      put(Command.HSTRLEN, true);
      put(Command.HVALS, true);
      put(JsonCommand.ARRINDEX, true);
      put(JsonCommand.ARRLEN, true);
      put(JsonCommand.GET, true);
      put(JsonCommand.MGET, true);
      put(JsonCommand.OBJKEYS, true);
      put(JsonCommand.OBJLEN, true);
      put(JsonCommand.STRLEN, true);
      put(JsonCommand.TYPE, true);
      put(Command.LCS, true);
      put(Command.LINDEX, true);
      put(Command.LLEN, true);
      put(Command.LPOS, true);
      put(Command.LRANGE, true);
      put(Command.MGET, true);
      put(Command.SCARD, true);
      put(Command.SDIFF, true);
      put(Command.SINTER, true);
      put(Command.SISMEMBER, true);
      put(Command.SMEMBERS, true);
      put(Command.SMISMEMBER, true);
      put(Command.STRLEN, true);
      put(Command.SUBSTR, true);
      put(Command.SUNION, true);
      put(TimeSeriesCommand.GET, true);
      put(TimeSeriesCommand.INFO, true);
      put(TimeSeriesCommand.RANGE, true);
      put(TimeSeriesCommand.REVRANGE, true);
      put(Command.TYPE, true);
      put(Command.XLEN, true);
      put(Command.XPENDING, true);
      put(Command.XRANGE, true);
      put(Command.XREVRANGE, true);
      put(Command.ZCARD, true);
      put(Command.ZCOUNT, true);
      put(Command.ZLEXCOUNT, true);
      put(Command.ZMSCORE, true);
      put(Command.ZRANGE, true);
      put(Command.ZRANGEBYLEX, true);
      put(Command.ZRANGEBYSCORE, true);
      put(Command.ZRANK, true);
      put(Command.ZREVRANGE, true);
      put(Command.ZREVRANGEBYLEX, true);
      put(Command.ZREVRANGEBYSCORE, true);
      put(Command.ZREVRANK, true);
      put(Command.ZSCORE, true);
    }
  };

  public DefaultClientSideCacheable() {
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    Boolean cachable = commandsToCache.get(command);
    return (cachable != null) ? cachable : false;
  }
}
