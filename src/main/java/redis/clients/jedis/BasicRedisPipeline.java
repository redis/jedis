package redis.clients.jedis;

import java.util.List;

/**
 * Pipelined responses for all of the low level, non key related commands
 */
public interface BasicRedisPipeline {

  Response<String> bgrewriteaof();

  Response<String> bgsave();

  Response<List<String>> configGet(String pattern);

  Response<String> configSet(String parameter, String value);

  Response<String> configResetStat();

  Response<String> save();

  Response<Long> lastsave();

  Response<String> flushDB();

  Response<String> flushAll();

  Response<String> info();

  Response<List<String>> time();

  Response<Long> dbSize();

  Response<String> shutdown();

  Response<String> ping();

  Response<String> select(int index);

  Response<String> swapDB(int index1, int index2);

  Response<String> migrate(String host, int port, String key, int destinationDB, int timeout);
}
