package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface MiscellaneousPipelineCommands {

  Response<Long> publish(final String channel, final String message);

  Response<LCSMatchResult> strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params);
}
