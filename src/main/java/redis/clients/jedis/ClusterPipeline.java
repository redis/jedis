package redis.clients.jedis;

import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.providers.JedisClusterConnectionProvider;
import redis.clients.jedis.resps.ScanResult;

import java.util.List;
import java.util.Set;

public class ClusterPipeline extends MultiNodePipelineBase implements PipelineCommands {

  private final JedisClusterConnectionProvider provider;
  private final RedisCommandObjects commandObjects;

  public ClusterPipeline(JedisClusterConnectionProvider provider) {
    this.provider = provider;
    this.commandObjects = new RedisCommandObjects();
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }

  @Override
  public Response<Boolean> exists(String key) {
    return appendCommand(provider.getNode(key), commandObjects.exists(key));
  }

  @Override
  public Response<Long> exists(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.exists(keys));
  }

  @Override
  public Response<Long> persist(String key) {
    return appendCommand(provider.getNode(key), commandObjects.persist(key));
  }

  @Override
  public Response<String> type(String key) {
    return appendCommand(provider.getNode(key), commandObjects.type(key));
  }

  @Override
  public Response<byte[]> dump(String key) {
    return appendCommand(provider.getNode(key), commandObjects.dump(key));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue) {
    return appendCommand(provider.getNode(key), commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return appendCommand(provider.getNode(key), commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public Response<Long> expire(String key, long seconds) {
    return appendCommand(provider.getNode(key), commandObjects.expire(key, seconds));
  }

  @Override
  public Response<Long> pexpire(String key, long milliseconds) {
    return appendCommand(provider.getNode(key), commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public Response<Long> expireAt(String key, long unixTime) {
    return appendCommand(provider.getNode(key), commandObjects.expireAt(key, unixTime));
  }

  @Override
  public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return appendCommand(provider.getNode(key), commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public Response<Long> ttl(String key) {
    return appendCommand(provider.getNode(key), commandObjects.ttl(key));
  }

  @Override
  public Response<Long> pttl(String key) {
    return appendCommand(provider.getNode(key), commandObjects.pttl(key));
  }

  @Override
  public Response<Long> touch(String key) {
    return appendCommand(provider.getNode(key), commandObjects.touch(key));
  }

  @Override
  public Response<Long> touch(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.touch(keys));
  }

  @Override
  public Response<List<String>> sort(String key) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key));
  }

  @Override
  public Response<Long> sort(String key, String dstKey) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, dstKey));
  }

  @Override
  public Response<List<String>> sort(String key, SortingParams sortingParameters) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, sortingParameters));
  }

  @Override
  public Response<Long> sort(String key, SortingParams sortingParameters, String dstKey) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, sortingParameters, dstKey));
  }

  @Override
  public Response<Long> del(String key) {
    return appendCommand(provider.getNode(key), commandObjects.del(key));
  }

  @Override
  public Response<Long> del(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.del(keys));
  }

  @Override
  public Response<Long> unlink(String key) {
    return appendCommand(provider.getNode(key), commandObjects.unlink(key));
  }

  @Override
  public Response<Long> unlink(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.unlink(keys));
  }

  @Override
  public Response<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    return appendCommand(provider.getNode(srcKey), commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public Response<String> rename(String oldKey, String newKey) {
    return appendCommand(provider.getNode(oldKey), commandObjects.rename(oldKey, newKey));
  }

  @Override
  public Response<Long> renamenx(String oldKey, String newKey) {
    return appendCommand(provider.getNode(oldKey), commandObjects.renamenx(oldKey, newKey));
  }

  @Override
  public Response<Long> memoryUsage(String key) {
    return appendCommand(provider.getNode(key), commandObjects.memoryUsage(key));
  }

  @Override
  public Response<Long> memoryUsage(String key, int samples) {
    return appendCommand(provider.getNode(key), commandObjects.memoryUsage(key, samples));
  }

  @Override
  public Response<Long> objectRefcount(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> objectEncoding(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> objectIdletime(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> objectFreq(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Set<String>> keys(String pattern) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> randomKey() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> get(String key) {
    return appendCommand(provider.getNode(key), commandObjects.get(key));
  }

  @Override
  public Response<String> set(String key, String value) {
    return appendCommand(provider.getNode(key), commandObjects.set(key, value));
  }

}
