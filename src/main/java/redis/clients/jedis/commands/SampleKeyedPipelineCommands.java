package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.FlushMode;

public interface SampleKeyedPipelineCommands {

  Response<Long> waitReplicas(String sampleKey, int replicas, long timeout);

  Response<Object> eval(String script, String sampleKey);

  Response<Object> evalsha(String sha1, String sampleKey);
//
//  Response<Boolean> scriptExists(String sha1, String sampleKey);

  Response<List<Boolean>> scriptExists(String sampleKey, String... sha1);

  Response<String> scriptLoad(String script, String sampleKey);

  Response<String> scriptFlush(String sampleKey);

  Response<String> scriptFlush(String sampleKey, FlushMode flushMode);

  Response<String> scriptKill(String sampleKey);
}
