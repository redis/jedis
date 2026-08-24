package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

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
import redis.clients.jedis.util.SafeEncoder;

/**
 * Guards the metadata rules behind {@link DefaultCacheable}: {@code readonly} flag, a key-name
 * argument, no {@code nondeterministic_output} / {@code dont_cache} tip, no {@code script_runner}
 * flag.
 */
public class DefaultCacheableTest {

  static ProtocolCommand[] cacheable() {
    return new ProtocolCommand[] { Command.GET, Command.MGET, Command.HGETALL, Command.LRANGE,
        Command.SMEMBERS, Command.ZSCORE, Command.GEOSEARCH, Command.XRANGE, Command.TYPE,
        // module commands: not excluded by prefix
        JsonCommand.GET, TimeSeriesCommand.RANGE, SearchCommand.SUGGET, BloomFilterCommand.EXISTS,
        CuckooFilterCommand.COUNT, CountMinSketchCommand.QUERY, TopKCommand.QUERY,
        TDigestCommand.QUANTILE, Command.VSIM, Command.ARGET,
        // added once metadata replaced the hand-written list
        Command.DIGEST, Command.EXPIRETIME, Command.PFCOUNT, Command.SINTERCARD,
        Command.ZDIFF, Command.ZINTERCARD, Command.ZUNION };
  }

  static ProtocolCommand[] notCacheable() {
    return new ProtocolCommand[] {
        // write commands
        Command.SET, Command.HSET, Command.XREADGROUP, Command.GETDEL, Command.GETEX,
        // read-only, but no key-name argument
        Command.KEYS, Command.SCAN, Command.DBSIZE, Command.RANDOMKEY,
        // nondeterministic_output
        Command.XPENDING, Command.SRANDMEMBER, Command.ZRANDMEMBER, Command.HRANDFIELD,
        // script_runner
        Command.EVAL_RO, Command.EVALSHA_RO, Command.FCALL_RO,
        // dont_cache
        TimeSeriesCommand.INFO, SearchCommand.SEARCH, SearchCommand.AGGREGATE,
        // blocking commands (rule 6)
        Command.XREAD,
        // metadata gaps overridden on the client side
        Command.TOUCH, Command.VRANDMEMBER,
        // BY/GET pattern keys are invisible to invalidation tracking
        Command.SORT_RO,
        // container commands: cannot be told apart from their non-cacheable subcommands
        Command.XINFO, Command.MEMORY };
  }

  @ParameterizedTest
  @MethodSource("cacheable")
  public void cacheableCommands(ProtocolCommand command) {
    assertTrue(DefaultCacheable.isDefaultCacheableCommand(command));
    assertTrue(DefaultCacheable.INSTANCE.isCacheable(command, Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("notCacheable")
  public void nonCacheableCommands(ProtocolCommand command) {
    assertFalse(DefaultCacheable.isDefaultCacheableCommand(command));
    assertFalse(DefaultCacheable.INSTANCE.isCacheable(command, Collections.emptyList()));
  }

  /** Commands with no generated verdict are denied (fail closed). */
  @Test
  public void unknownCommandIsDenied() {
    ProtocolCommand unknown = () -> SafeEncoder.encode("NO.SUCH.COMMAND");
    assertFalse(DefaultCacheable.isDefaultCacheableCommand(unknown));
    assertFalse(DefaultCacheable.INSTANCE.isCacheable(unknown, Collections.emptyList()));
  }

  /** Blocking commands must never be served from the cache. */
  @ParameterizedTest
  @EnumSource(value = Command.class, names = { "BLPOP", "BRPOP", "BLMOVE", "BLMPOP", "BZMPOP",
      "BZPOPMIN", "BZPOPMAX", "BRPOPLPUSH", "XREAD", "XREADGROUP" })
  public void blockingCommandsAreNotCacheable(Command command) {
    assertFalse(DefaultCacheable.isDefaultCacheableCommand(command));
  }
}
