package redis.clients.jedis.commands;

import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface MiscellaneousBinaryCommands {

  long publish(byte[] channel, byte[] message);

  LCSMatchResult strAlgoLCSStrings(final byte[] strA, final byte[] strB, final StrAlgoLCSParams params);
}
