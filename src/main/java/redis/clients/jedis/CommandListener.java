package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

public interface CommandListener {

  void afterCommand(Connection conn, ProtocolCommand cmd, final byte[][] args);

}
