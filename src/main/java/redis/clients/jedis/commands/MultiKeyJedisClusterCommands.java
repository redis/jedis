package redis.clients.jedis.commands;

import redis.clients.jedis.ScanResult;

import java.util.List;

public interface MultiKeyJedisClusterCommands extends MultiKeyCommands {

  @Override
  default ScanResult<String> scan(String cursor) {
    throw new UnsupportedOperationException();
  }

  @Override
  default String randomKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  default List<String> blpop(String... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  default List<String> brpop(String... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  default String watch(String... keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  default String unwatch() {
    throw new UnsupportedOperationException();
  }

}
