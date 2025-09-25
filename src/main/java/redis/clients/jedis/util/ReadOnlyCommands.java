package redis.clients.jedis.util;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.bloom.RedisBloomProtocol.BloomFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CountMinSketchCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CuckooFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TDigestCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TopKCommand;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ReadOnlyCommands {

  private static final ReadOnlyPredicate PREDICATE = command -> isReadOnlyCommand(command);

  private static final Set<ProtocolCommand> READ_ONLY_COMMANDS = new HashSet<ProtocolCommand>(Arrays.asList(
                  // string
                  Command.PING, Command.AUTH, Command.HELLO, Command.GET, Command.EXISTS, Command.TYPE,
                  Command.KEYS, Command.RANDOMKEY, Command.DUMP, Command.DBSIZE, Command.SELECT, Command.ECHO,
                  Command.EXPIRETIME, Command.PEXPIRETIME, Command.TTL, Command.PTTL, Command.SORT_RO,
                  Command.INFO, Command.MONITOR, Command.LCS, Command.MGET, Command.STRLEN, Command.SUBSTR,
                  // bit
                  Command.GETBIT, Command.BITPOS, Command.GETRANGE, Command.BITCOUNT, Command.BITFIELD_RO,
                  // hash
                  Command.HGET, Command.HMGET, Command.HEXISTS, Command.HLEN, Command.HKEYS, Command.HVALS,
                  Command.HGETALL, Command.HSTRLEN, Command.HTTL, Command.HPTTL, Command.HEXPIRETIME,
                  Command.HPEXPIRETIME, Command.HRANDFIELD,
                  // list
                  Command.LLEN, Command.LRANGE, Command.LINDEX, Command.LPOS,
                  // set
                  Command.SMEMBERS, Command.SCARD, Command.SRANDMEMBER, Command.SINTER, Command.SUNION,
                  Command.SDIFF, Command.SISMEMBER, Command.SMISMEMBER, Command.SINTERCARD,
                  // zset
                  Command.ZDIFF, Command.ZRANGE, Command.ZRANK, Command.ZREVRANK, Command.ZREVRANGE,
                  Command.ZRANDMEMBER, Command.ZCARD, Command.ZSCORE, Command.ZCOUNT, Command.ZUNION,
                  Command.ZINTER, Command.ZRANGEBYSCORE, Command.ZREVRANGEBYSCORE, Command.ZLEXCOUNT,
                  Command.ZRANGEBYLEX, Command.ZREVRANGEBYLEX, Command.ZMSCORE, Command.ZINTERCARD,
                  // geo
                  Command.GEODIST, Command.GEOHASH, Command.GEOPOS, Command.GEORADIUS_RO,
                  Command.GEORADIUSBYMEMBER_RO,
                  // hyper log
                  Command.PFCOUNT,
                  // stream
                  Command.XLEN, Command.XRANGE, Command.XREVRANGE, Command.XREAD, Command.XREADGROUP,
                  Command.XPENDING, Command.XINFO,
                  // program
                  Command.FCALL_RO,
                  // vector set
                  Command.LASTSAVE, Command.ROLE, Command.OBJECT, Command.TIME, Command.SCAN, Command.HSCAN,
                  Command.SSCAN, Command.ZSCAN, Command.LOLWUT, Command.VSIM, Command.VDIM, Command.VCARD,
                  Command.VEMB, Command.VLINKS, Command.VRANDMEMBER, Command.VGETATTR, Command.VINFO,
                  // BloomFilterCommand
                  BloomFilterCommand.EXISTS, BloomFilterCommand.MEXISTS, BloomFilterCommand.CARD,
                  BloomFilterCommand.INFO,
                  // CuckooFilterCommand
                  CuckooFilterCommand.EXISTS, CuckooFilterCommand.MEXISTS, CuckooFilterCommand.COUNT,
                  CuckooFilterCommand.INFO,
                  // CountMinSketchCommand
                  CountMinSketchCommand.QUERY, CountMinSketchCommand.INFO,
                  // TopKCommand
                  TopKCommand.QUERY, TopKCommand.LIST, TopKCommand.INFO,
                  // TDigestCommand
                  TDigestCommand.INFO, TDigestCommand.CDF, TDigestCommand.QUANTILE, TDigestCommand.MIN,
                  TDigestCommand.MAX, TDigestCommand.TRIMMED_MEAN, TDigestCommand.RANK,
                  TDigestCommand.REVRANK, TDigestCommand.BYRANK, TDigestCommand.BYREVRANK,
                  // JsonCommand
                  JsonCommand.GET, JsonCommand.MGET, JsonCommand.TYPE, JsonCommand.STRLEN,
                  JsonCommand.ARRINDEX, JsonCommand.ARRLEN, JsonCommand.OBJKEYS, JsonCommand.OBJLEN,
                  JsonCommand.DEBUG, JsonCommand.RESP,
                  // SearchCommand
                  SearchCommand.INFO, SearchCommand.SEARCH, SearchCommand.EXPLAIN, SearchCommand.EXPLAINCLI,
                  SearchCommand.AGGREGATE, SearchCommand.CURSOR, SearchCommand.SYNDUMP, SearchCommand.SUGGET,
                  SearchCommand.SUGLEN, SearchCommand.DICTDUMP, SearchCommand.SPELLCHECK,
                  SearchCommand.TAGVALS, SearchCommand.PROFILE, SearchCommand._LIST,
                  // TimeSeriesCommand
                  TimeSeriesCommand.RANGE, TimeSeriesCommand.REVRANGE, TimeSeriesCommand.MRANGE,
                  TimeSeriesCommand.MREVRANGE, TimeSeriesCommand.INFO, TimeSeriesCommand.GET,
                  TimeSeriesCommand.MGET, TimeSeriesCommand.QUERYINDEX
          ));

  public static ReadOnlyPredicate asPredicate() {
    return PREDICATE;
  }

  public static boolean isReadOnlyCommand(CommandArguments args) {
    return READ_ONLY_COMMANDS.contains(args.getCommand());
  }

  @FunctionalInterface
  public interface ReadOnlyPredicate {

    /**
     * @param command the input command.
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    boolean isReadOnly(CommandArguments command);
  }
}
