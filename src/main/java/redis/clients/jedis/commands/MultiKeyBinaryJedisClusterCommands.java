package redis.clients.jedis.commands;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

public interface MultiKeyBinaryJedisClusterCommands extends MultiKeyBinaryCommands {

  @Override
  default List<byte[]> blpop(byte[]... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  default List<byte[]> brpop(byte[]... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  default String watch(byte[]... keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  default String unwatch() {
    throw new UnsupportedOperationException();
  }

  @Override
  default byte[] randomBinaryKey() {
    throw new UnsupportedOperationException();
  }

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params);
}
