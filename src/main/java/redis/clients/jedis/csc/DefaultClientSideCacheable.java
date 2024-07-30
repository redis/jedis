package redis.clients.jedis.csc;

import java.util.EnumSet;
import java.util.List;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;

public class DefaultClientSideCacheable implements ClientSideCacheable {

  public static final DefaultClientSideCacheable INSTANCE = new DefaultClientSideCacheable();

  private static final EnumSet<Command> CSC_COMMANDS = EnumSet.of(
      Command.BITCOUNT,
      Command.BITFIELD_RO,
      Command.BITPOS,
      Command.EXISTS,
      Command.GEODIST,
      Command.GEOHASH,
      Command.GEOPOS,
      Command.GEORADIUSBYMEMBER_RO,
      Command.GEORADIUS_RO,
      Command.GEOSEARCH,
      Command.GET,
      Command.GETBIT,
      Command.GETRANGE,
      Command.HEXISTS,
      Command.HGET,
      Command.HGETALL,
      Command.HKEYS,
      Command.HLEN,
      Command.HMGET,
      Command.HSTRLEN,
      Command.HVALS,
      Command.LCS,
      Command.LINDEX,
      Command.LLEN,
      Command.LPOS,
      Command.LRANGE,
      Command.MGET,
      Command.SCARD,
      Command.SDIFF,
      Command.SINTER,
      Command.SISMEMBER,
      Command.SMEMBERS,
      Command.SMISMEMBER,
      Command.STRLEN,
      Command.SUBSTR,
      Command.SUNION,
      Command.TYPE,
      Command.XLEN,
      Command.XPENDING,
      Command.XRANGE,
      Command.XREVRANGE,
      Command.ZCARD,
      Command.ZCOUNT,
      Command.ZLEXCOUNT,
      Command.ZMSCORE,
      Command.ZRANGE,
      Command.ZRANGEBYLEX,
      Command.ZRANGEBYSCORE,
      Command.ZRANK,
      Command.ZREVRANGE,
      Command.ZREVRANGEBYLEX,
      Command.ZREVRANGEBYSCORE,
      Command.ZREVRANK,
      Command.ZSCORE
  );

  public DefaultClientSideCacheable() { }

  @Override
  public boolean isCacheable(ProtocolCommand command, List keys) {
    return command.getClass() == Command.class && CSC_COMMANDS.contains((Command) command);
  }
}
