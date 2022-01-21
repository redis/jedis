package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.MigrateParams;

public interface DatabasePipelineCommands {

  Response<String> select(int index);

  Response<Long> dbSize();

  Response<String> swapDB(int index1, int index2);

  Response<Long> move(String key, int dbIndex);

  Response<Long> move(byte[] key, int dbIndex);

  Response<Boolean> copy(String srcKey, String dstKey, int db, boolean replace);

  Response<Boolean> copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  Response<String> migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  Response<String> migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, byte[]... keys);

  Response<String> migrate(String host, int port, String key, int destinationDB, int timeout);

  Response<String> migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, String... keys);

}
