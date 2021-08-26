package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

/**
 * Default JedisProtocol implementation, uses Protocol.
 */
public class JedisProtocol {
  public static final JedisProtocol DEFAULT = new JedisProtocol();

  public void sendCommand(final RedisOutputStream os, final ProtocolCommand command,
      final byte[]... args) {
    Protocol.sendCommand(os, command, args);
  }

  public String readErrorLineIfPossible(RedisInputStream is) {
    return Protocol.readErrorLineIfPossible((is));
  }

  public Object read(RedisInputStream is) {
    return Protocol.read(is);
  }
}
