package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface MiscellaneousPipelineBinaryCommands {

  Response<Long> publish(byte[] channel, byte[] message);

  Response<LCSMatchResult> strAlgoLCSStrings(final byte[] strA, final byte[] strB, final StrAlgoLCSParams params);
}
