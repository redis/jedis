package redis.clients.jedis.commands;

import java.util.List;

public interface SampleBinaryKeyedCommands {

  long waitReplicas(byte[] sampleKey, int replicas, long timeout);

  Object eval(byte[] script, byte[] sampleKey);

  Object evalsha(byte[] sha1, byte[] sampleKey);

  Boolean scriptExists(byte[] sha1, byte[] sampleKey);

  List<Boolean> scriptExists(byte[] sampleKey, byte[]... sha1);

  String scriptLoad(byte[] script, byte[] sampleKey);

  String scriptFlush(byte[] sampleKey);

  String scriptKill(byte[] sampleKey);
}
