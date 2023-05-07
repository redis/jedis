package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.util.KeyValue;

public interface SampleBinaryKeyedCommands {

  long waitReplicas(byte[] sampleKey, int replicas, long timeout);

  KeyValue<Long, Long> waitAOF(byte[] sampleKey, long numLocal, long numReplicas, long timeout);

  Object eval(byte[] script, byte[] sampleKey);

  Object evalsha(byte[] sha1, byte[] sampleKey);

  Boolean scriptExists(byte[] sha1, byte[] sampleKey);

  List<Boolean> scriptExists(byte[] sampleKey, byte[]... sha1s);

  byte[] scriptLoad(byte[] script, byte[] sampleKey);

  String scriptFlush(byte[] sampleKey);

  String scriptFlush(byte[] sampleKey, FlushMode flushMode);

  String scriptKill(byte[] sampleKey);
}
