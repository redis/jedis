package redis.clients.jedis;

import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.commands.ProtocolCommand;

public class ClusterCommandArguments extends CommandArguments {

  private int commandHashSlot = -1;

  public ClusterCommandArguments(ProtocolCommand command) {
    super(command);
  }

  public int getCommandHashSlot() {
    return commandHashSlot;
  }

  @Override
  protected void processKey(byte[] key) {
    super.processKey(key); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void processKey(String key) {
    final int hashSlot = JedisClusterCRC16.getSlot(key);
    if (commandHashSlot < 0) {
      commandHashSlot = hashSlot;
    } else if (commandHashSlot != hashSlot) {
      throw new IllegalArgumentException(key);
    }
  }

}
