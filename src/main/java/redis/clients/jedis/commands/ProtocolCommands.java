package redis.clients.jedis.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.bloom.RedisBloomProtocol;
import redis.clients.jedis.json.JsonProtocol;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.timeseries.TimeSeriesProtocol;

@Internal
public final class ProtocolCommands {

  private static final List<ProtocolCommand[]> PROTOCOL_ENUMS = Collections
      .unmodifiableList(Arrays.asList(Protocol.Command.values(), JsonProtocol.JsonCommand.values(),
        SearchProtocol.SearchCommand.values(), TimeSeriesProtocol.TimeSeriesCommand.values(),
        RedisBloomProtocol.BloomFilterCommand.values(),
        RedisBloomProtocol.CuckooFilterCommand.values(),
        RedisBloomProtocol.CountMinSketchCommand.values(), RedisBloomProtocol.TopKCommand.values(),
        RedisBloomProtocol.TDigestCommand.values()));

  private ProtocolCommands() {
  }

  public static List<ProtocolCommand[]> allCommands() {
    return PROTOCOL_ENUMS;
  }
}
