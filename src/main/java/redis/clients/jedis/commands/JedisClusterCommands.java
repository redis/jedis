package redis.clients.jedis.commands;

import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;

import java.util.List;
import java.util.Map;

public interface JedisClusterCommands extends JedisCommands {

  @Override
  default Long move(String key, int dbIndex) {
    throw new UnsupportedOperationException();
  }

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   * 
   * @param key
   * @param groupname
   * @param cosumer
   * @param count
   * @param block
   * @param streams
   * @return
   */
  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer, int count, long block, boolean noAck, Map.Entry<String, StreamEntryID>... streams);

  Long waitReplicas(final String key, final int replicas, final long timeout);

  @Override
  default Object sendCommand(ProtocolCommand cmd, String... args) {
    throw new UnsupportedOperationException();
  }

  Object sendCommand(final String sampleKey, ProtocolCommand cmd, String... args);

}
