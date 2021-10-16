package redis.clients.jedis.commands;

import java.util.List;

public interface SampleKeyedCommands {

  long waitReplicas(String key, int replicas, long timeout);

  Object eval(String script, String sampleKey);

  Object evalsha(String sha1, String sampleKey);

  Boolean scriptExists(String sha1, String sampleKey);

  List<Boolean> scriptExists(String sampleKey, String... sha1);

  String scriptLoad(String script, String sampleKey);

  String scriptFlush(String sampleKey);

  String scriptKill(String sampleKey);
}
