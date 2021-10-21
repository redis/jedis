package redis.clients.jedis.commands;

import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface MiscellaneousCommands {

  long publish(final String channel, final String message);

  LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params);
}
